import {base,Client} from './http-client.mjs';
import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {resolve} from 'node:path';
import {checkPortalLayout} from './check-portal-layout.mjs';
const prefix='PT'+randomBytes(3).toString('hex').toUpperCase();
const password='Portal-test-2026!',updated='Portal-new-2026!';let checks=0;
function fixture(action){
 const r=spawnSync('powershell.exe',['-NoProfile','-ExecutionPolicy','Bypass','-File',resolve('scripts/portal-fixture.ps1'),'-Action',action,'-Prefix',prefix],{encoding:'utf8',windowsHide:true});
 if(r.status!==0)throw new Error('Fixture '+action+': '+r.stderr);return r.stdout.trim();
}

const token=p=>{const m=p.html.match(/name="csrfToken" value="([^"]+)"/);assert.ok(m,'CSRF rendered');return m[1];};
async function page(c,path){const p=await c.request(path);assert.equal(p.status,200,path+' '+p.html.slice(0,400));return p;}
async function login(c,role,name,pwd){const p=await page(c,'/auth/login?role='+role);const r=await c.request('/auth/login',{csrfToken:token(p),role,username:name,password:pwd});assert.equal(r.status,303,'Login '+role);}
async function post(c,path,form,success=true){const p=await page(c,path.split('?')[0]);const r=await c.request(path,{csrfToken:token(p),...form});if(success)assert.equal(r.status,303,path+' '+r.html.slice(0,700));else assert.ok(r.status>=400&&r.status<500,path+' should reject');return r;}
const idFrom=(r)=>Number(new URL(r.location,base).searchParams.get('id'));
const pass=s=>{checks++;console.log('PASS '+s);};
const admin=new Client(),students=Array.from({length:6},()=>new Client());
let created=false;
async function run(){
try {
 const data=JSON.parse(fixture('create'));created=true;const room=data.room;
 await login(admin,'admin','admin',process.env.KTX_ADMIN_PASSWORD||'123');
 for(let i=0;i<6;i++)await login(students[i],'student',prefix+String(i+1).padStart(2,'0'),password);
 const student=students[0],other=students[1];
 for(const key of ['home','profile','rooms','register','history','contracts','invoices','payments','repairs','notifications','password']){
  const p=await page(student,'/student/'+key);assert.ok(!p.html.includes('preview-notice'),'No read-only student preview');
 }
 for(const key of ['registrations','contracts','invoices','payments','repairs','notifications'])await page(admin,'/admin/'+key);
 const roomPage=await page(student,'/student/rooms');
 assert.equal((roomPage.html.match(/class="room-card"/g)||[]).length,12,'Rooms display twelve per page');
 const nextRooms=await page(student,'/student/rooms?page=2&zone=A');
 assert.ok(nextRooms.html.includes('Trang 2 / 5'),'Filtered pagination counts fifty rooms');
 assert.ok(nextRooms.html.includes('zone=A'),'Pagination keeps selected zone');
 pass('All student sections and supporting admin pages compile and render');
 assert.equal((await student.request('/student/profile',{action:'update',phone:'0912345678'})).status,403);
 assert.equal((await student.request('/admin/invoices')).status,403);assert.equal((await admin.request('/student/profile')).status,403);
 await post(student,'/student/profile',{action:'update',phone:'0912345678'});assert.ok((await page(student,'/student/profile')).html.includes('0912345678'));
 await post(student,'/student/profile',{action:'update',phone:'bad'},false);
 pass('CSRF, roles and phone persistence');
 await post(student,'/student/repairs',{action:'submit',description:'No contract'},false);
 await post(students[5],'/student/register',{action:'submit',room},false);
 let registration=idFrom(await post(student,'/student/register',{action:'submit',room}));
 await post(student,'/student/register',{action:'submit',room},false);
 await post(other,'/student/history',{action:'cancel',id:registration},false);
 await post(student,'/student/history',{action:'cancel',id:registration});
 registration=idFrom(await post(student,'/student/register',{action:'submit',room}));
 await post(admin,'/admin/registrations',{action:'reject',id:registration,reason:'Thử từ chối'});
 assert.ok((await page(student,'/student/history')).html.includes('Thử từ chối'));
 registration=idFrom(await post(student,'/student/register',{action:'submit',room}));
 const today=new Date().toLocaleDateString('en-CA',{timeZone:'Asia/Ho_Chi_Minh'}),[year,month]=today.split('-');
 const contract=idFrom(await post(admin,'/admin/registrations',{action:'approve',id:registration,start:today,end:''}));
 await post(student,'/student/register',{action:'submit',room},false);
 assert.equal((await other.request('/student/contracts?id='+contract)).status,404);
 await page(student,'/student/contracts?id='+contract+'&print=1');
 pass('Gender, one pending request, owned cancellation, rejection and contract creation');
 await post(admin,'/admin/invoices',{action:'create',id:contract,month:Number(month),year,electricity:'50000',water:'25000',due:today});
 await post(admin,'/admin/invoices',{action:'create',id:contract,month:Number(month),year,electricity:'0',water:'0',due:today},false);
 const invoices=await page(student,'/student/invoices');const invoice=Number(invoices.html.match(/\/student\/invoices\?id=(\d+)/)?.[1]);assert.ok(invoice);
 assert.ok(invoices.html.includes('775.000'));assert.equal((await other.request('/student/invoices?id='+invoice)).status,404);
 await post(other,'/student/payments',{action:'transfer',invoice,amount:'1',reference:prefix+'-FOREIGN'},false);
 await post(student,'/student/payments',{action:'transfer',invoice,amount:'100000',reference:prefix+'-TX1'});
 const firstPayment=Number((await page(admin,'/admin/payments')).html.match(/name="id" value="(\d+)"/)[1]);
 let detail=await page(student,'/student/invoices?id='+invoice);assert.ok(detail.html.includes('775.000'));assert.ok(detail.html.includes('100.000'));
 if(process.env.PORTAL_VISUAL)await checkPortalLayout(base,student.cookies.get('JSESSIONID'),admin.cookies.get('JSESSIONID'),invoice);
 if(process.env.PORTAL_LAYOUT_ONLY){console.log('LAYOUT_CHECKS_PASS');return;}
 await post(admin,'/admin/payments',{action:'confirm',id:firstPayment});
 await post(admin,'/admin/payments',{action:'confirm',id:firstPayment},false);
 assert.ok((await page(student,'/student/invoices?id='+invoice)).html.includes('675.000'));
 await post(student,'/student/payments',{action:'transfer',invoice,amount:'675001',reference:prefix+'-OVER'},false);
 await post(student,'/student/payments',{action:'transfer',invoice,amount:'675000',reference:''});
 assert.ok((await page(student,'/student/payments')).html.includes('DEMO-'));
 await post(student,'/student/payments',{action:'transfer',invoice,amount:'1',reference:prefix+'-EXTRA'},false);
 const secondPayment=Number((await page(admin,'/admin/payments')).html.match(/name="id" value="(\d+)"/)[1]);
 await post(admin,'/admin/payments',{action:'reject',id:secondPayment});
 await post(admin,'/admin/payments',{action:'cash',id:invoice,amount:'675000'});
 await post(admin,'/admin/payments',{action:'cash',id:invoice,amount:'1'},false);
 assert.ok((await page(student,'/student/invoices?id='+invoice)).html.includes('Đã thanh toán'));
 pass('Monthly uniqueness, invoice ownership, pending reservation, exact confirmed sums, rejection and cash');
 await post(student,'/student/repairs',{action:'submit',description:'Vòi nước hỏng <script>demo</script>',room:'C101'});
 const repairs=await page(admin,'/admin/repairs');const repair=Number(repairs.html.match(/name="id" value="(\d+)"/)[1]);
 assert.ok(repairs.html.includes('&lt;script&gt;'));
 await post(admin,'/admin/repairs',{action:'update',id:repair,status:'DANG_XU_LY'});
 assert.ok((await page(student,'/student/repairs')).html.includes('Đang xử lý'));
 await post(admin,'/admin/notifications',{action:'publish',title:'Portal '+prefix,body:'Thông báo thử '+prefix});
 assert.ok((await page(student,'/student/notifications?q='+prefix)).html.includes('Thông báo thử '+prefix));
 pass('Repair targets current room, escapes content, shows progress, and announcements are searchable');
 for(let i=1;i<=2;i++){
  const r=idFrom(await post(students[i],'/student/register',{action:'submit',room}));
  await post(admin,'/admin/registrations',{action:'approve',id:r,start:today,end:''});
 }
 const candidates=[];for(let i=3;i<=4;i++)candidates.push(idFrom(await post(students[i],'/student/register',{action:'submit',room})));
 const adminPage=await page(admin,'/admin/registrations');const approvals=await Promise.all(candidates.map(id=>admin.request('/admin/registrations',{csrfToken:token(adminPage),action:'approve',id,start:today,end:''})));
 assert.equal(approvals.filter(r=>r.status===303).length,1);assert.equal(approvals.filter(r=>r.status===400||r.status===409).length,1);
 pass('Concurrent approvals cannot allocate the last bed twice');
 await post(admin,'/admin/contracts',{action:'finish',id:contract});
 await post(student,'/student/repairs',{action:'submit',description:'After end'},false);
 await post(student,'/student/password',{action:'change',currentPassword:'wrong',newPassword:updated,confirmPassword:updated},false);
 await post(student,'/student/password',{action:'change',currentPassword:password,newPassword:updated,confirmPassword:updated});
 assert.equal((await student.request('/student/profile')).status,302);
 await login(student,'student',prefix+'01',updated);
 pass('Contract termination removes repair eligibility; password update requires re-login');
 console.log('STUDENT_PORTAL_CHECKS='+checks);
} finally {fixture('cleanup');console.log('Unique '+prefix+' fixture records cleaned; existing users preserved.');}
}
await run();
