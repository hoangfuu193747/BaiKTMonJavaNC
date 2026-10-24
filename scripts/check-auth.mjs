import {base,Client} from './http-client.mjs';
import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {resolve,delimiter} from 'node:path';

if(!process.env.KTX_ADMIN_PASSWORD||!process.env.KTX_DB_PASSWORD)throw new Error('Set KTX_ADMIN_PASSWORD and KTX_DB_PASSWORD.');
const prefix='QA'+randomBytes(3).toString('hex').toUpperCase();
const s1=prefix+'S1',s2=prefix+'S2',s3=prefix+'S3',password=randomBytes(18).toString('base64url');
const email=id=>id+'@example.test';
const classpath=['target/test-classes','target/classes','target/QuanLyKyTucXa/WEB-INF/lib/*'].map(p=>resolve(p)).join(delimiter);
let checks=0;
function fixture(action){
    const result=spawnSync('java',['-cp',classpath,'com.qlktx.util.AuthFixtureTool',action,prefix],{encoding:'utf8',windowsHide:true});
    if(result.status!==0)throw new Error('Fixture '+action+' failed: '+result.stderr);
    return result.stdout;
}

function pass(label){checks++;console.log('PASS '+label);}
function token(page){const match=page.html.match(/name="csrfToken" value="([^"]+)"/);assert.ok(match,'CSRF token rendered');return match[1];}
async function page(client,path,expected){
    const result=await client.request(path);assert.equal(result.status,200,path);
    if(expected)assert.ok(expected.test(result.html),path+' must render '+expected);return result;
}
async function login(client,role,name,secret){
    const form=await page(client,'/auth/login?role='+role);
    return client.request('/auth/login',{csrfToken:token(form),role,username:name,password:secret});
}
const adminKeys=['home','students','zones','rooms','registrations','contracts','invoices','payments','repairs','notifications','reports'];
const studentKeys=['home','profile','rooms','register','history','contracts','invoices','payments','repairs','notifications'];
const admin=new Client(),student=new Client(),other=new Client(),anonymous=new Client();
try{
    fixture('create');
    assert.equal((await anonymous.request('/admin/home')).status,302);
    assert.equal((await anonymous.request('/student/home')).status,302);
    assert.equal((await anonymous.request('/preview')).status,404);
    pass('Anonymous users cannot open workspaces or internal preview servlet');
    let form=await page(student,'/auth/register',/name="email"/);
    let csrf=token(form);
    assert.equal((await student.request('/auth/register',{action:'lookup',studentId:s1,email:email(s1)})).status,403);
    pass('Registration POST requires CSRF');
    for(const data of [{studentId:'NO-SUCH',email:email(s1)},{studentId:s1,email:email(s2)},{studentId:s3,email:email(s3)}]){
        const result=await student.request('/auth/register',{csrfToken:csrf,action:'lookup',...data});
        assert.equal(result.status,200);assert.ok(/không khớp/.test(result.html));assert.ok(!/name="confirmPassword"/.test(result.html));
    }
    pass('Unknown student, another student email and inactive roster are rejected before profile display');
    let result=await student.request('/auth/register',{csrfToken:csrf,action:'lookup',studentId:s1,email:email(s1)});
    assert.equal(result.status,200);assert.ok(/name="confirmPassword"/.test(result.html));assert.ok(result.html.includes(s1));
    pass('Matching MaSV and Gmail opens account creation form');
    const signup={csrfToken:csrf,action:'register',studentId:s1,email:email(s1),phone:'0912345678',password,confirmPassword:password};
    result=await student.request('/auth/register',{...signup,email:email(s2)});assert.ok(/không khớp/.test(result.html));
    result=await student.request('/auth/register',{...signup,confirmPassword:'different'});assert.ok(/xác nhận chưa khớp/.test(result.html));
    pass('Direct signup rechecks roster and password confirmation');
    result=await student.request('/auth/register',{...signup,role:'admin',VaiTro:'ADMIN'});
    assert.equal(result.status,303);assert.ok(result.location.includes('registered=1'));
    result=await student.request('/auth/register',signup);assert.ok(/đã có tài khoản/.test(result.html));
    pass('Student account created; duplicate rejected despite spoofed role input');
    form=await page(other,'/auth/register');csrf=token(form);
    const second={csrfToken:csrf,action:'register',studentId:s2,email:email(s2),phone:'',password,confirmPassword:password};
    const concurrent=await Promise.all([other.request('/auth/register',second),other.request('/auth/register',second)]);
    assert.equal(concurrent.filter(r=>r.status===303).length,1);
    assert.equal(concurrent.filter(r=>r.status===200&&/đã có tài khoản/.test(r.html)).length,1);
    const inspected=fixture('inspect');assert.ok(inspected.includes('SINH_VIEN|60|1|'+s1));assert.ok(inspected.includes('SINH_VIEN|60|1|'+s2));
    pass('Atomic signup creates TaiKhoan + SinhVien once, with BCrypt and SINH_VIEN role');
    result=await login(student,'student',s1,'wrong');assert.equal(result.status,401);
    const before=student.cookies.get('JSESSIONID');
    result=await login(student,'student',s1,password);assert.equal(result.status,303);assert.notEqual(student.cookies.get('JSESSIONID'),before);
    const old=new Client();old.cookies.set('JSESSIONID',before);assert.equal((await old.request('/student/home')).status,302);
    pass('Student BCrypt login succeeds and rotates session; incorrect password fails');
    result=await login(other,'student',s2,password);assert.equal(result.status,303);
    for(const key of studentKeys){
        const output=await page(student,'/student/'+key);
        assert.ok(output.html.includes(s1));assert.ok(!output.html.includes(s2),'Student must not see another profile');
    }
    form=await page(student,'/student/profile?id='+s2);assert.ok(form.html.includes(s1));assert.ok(!form.html.includes(s2));
    pass('All ten student sections render real data within the current student identity');
    const rooms=await page(student,'/student/rooms');assert.ok(rooms.html.includes('A101'));assert.ok((await page(student,'/student/rooms?q=D510')).html.includes('D510'));assert.ok(rooms.html.includes('700.000'));
    pass('Room search reads current database rooms and price');
    assert.equal((await student.request('/admin/home')).status,403);
    assert.equal((await student.request('/admin/students',{action:'create'})).status,403);
    pass('Student cannot access admin GET or POST routes');
    result=await login(admin,'admin','admin',process.env.KTX_ADMIN_PASSWORD);
    assert.equal(result.status,303,'Seed Admin BCrypt password should authenticate');
    pass('Existing Admin logs in using TaiKhoan BCrypt hash');
    for(const key of adminKeys)await page(admin,'/admin/'+key);
    const adminProfile=await page(admin,'/admin/profile',/Tên đăng nhập/);
    assert.ok(adminProfile.html.includes('Hồ sơ cá nhân'));
    assert.ok(!adminProfile.html.includes('MatKhauHash'));
    const studentsTable=await page(admin,'/admin/students');
    const heading=studentsTable.html.match(/<thead>([\s\S]*?)<\/thead>/)?.[1]||'';
    assert.ok(/Sinh vi\u00ean/.test(heading)&&heading.includes('Email'));
    assert.ok(!heading.includes('JSESSIONID')&&!heading.includes('sec-fetch')&&!heading.includes('Cookie'));
    assert.equal((await admin.request('/admin/profile',{csrfToken:token(adminProfile),action:'update'})).status,400);
    pass('Admin dropdown profile is read-only and table headings never render HTTP headers');
    assert.equal((await admin.request('/student/home')).status,403);
    pass('All eleven admin sections render; admin cannot use student routes');
    assert.equal((await admin.request('/admin/unknown')).status,404);
    assert.equal((await admin.request('/WEB-INF/views/student/portal/home.jsp')).status,404);
    const adminRooms=await page(admin,'/admin/rooms');assert.ok(!adminRooms.html.includes('MatKhauHash'));
    pass('Views cannot be opened directly and password hashes are never displayed');

    fixture('lock');
    assert.equal((await student.request('/student/home')).status,302);
    result=await login(student,'student',s1,password);assert.equal(result.status,401);
    pass('Locked student loses existing session access and cannot log in again');
    form=await page(other,'/student/home');
    result=await other.request('/auth/logout',{csrfToken:token(form)});assert.equal(result.status,303);
    assert.equal((await other.request('/student/home')).status,302);
    form=await page(admin,'/admin/home');
    result=await admin.request('/auth/logout',{csrfToken:token(form)});assert.equal(result.status,303);
    assert.equal((await admin.request('/admin/home')).status,302);
    pass('Logout invalidates both roles');
}finally{fixture('cleanup');console.log('Unique auth fixtures cleaned; seed students and admin preserved.');}
console.log('AUTH_CHECKS:'+checks);
