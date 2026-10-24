import {base,Client} from './http-client.mjs';
import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {resolve} from 'node:path';
import {checkAdminLayout} from './check-admin-layout.mjs';


const prefix='PT'+randomBytes(3).toString('hex').toUpperCase();
let data,restoreAdminPassword=false,checks=0;
function fixture(action){
 const args=['-NoProfile','-ExecutionPolicy','Bypass','-File',resolve('scripts/portal-fixture.ps1'),'-Action',action,'-Prefix',prefix,'-ForManagement'];
 if(action==='create')args.push('-ForVisual');
 if(action==='cleanup'&&data)args.push('-ManagementRoom',data.room,'-OriginalZoneName',data.zoneName);
 if(action==='cleanup'&&restoreAdminPassword)args.push('-OriginalAdminHash',data.adminHash,'-OriginalAdminUsername',data.adminUsername);
 const result=spawnSync('powershell.exe',args,{encoding:'utf8',windowsHide:true});
 if(result.error)throw result.error;
 if(result.status!==0)throw new Error('Fixture '+action+': '+result.stderr);
 return result.stdout.trim();
}

const token=response=>{const value=response.html.match(/name="csrfToken" value="([^"]+)"/);assert.ok(value,'CSRF token');return value[1];};
async function page(client,path){const response=await client.request(path);assert.equal(response.status,200,path+' '+response.html.slice(0,600));return response;}
async function login(client,role,username,password){const response=await page(client,'/auth/login?role='+role);assert.equal((await client.request('/auth/login',{csrfToken:token(response),role,username,password})).status,303,'Login '+role);}
async function post(client,path,form,expected=303){const response=await client.request(path,{csrfToken:token(await page(client,path)),...form});assert.equal(response.status,expected,path+' '+JSON.stringify(form.action)+' '+response.html.slice(0,600));return response;}
function pass(message){checks++;console.log('PASS '+message);}
const admin=new Client(),student=new Client();
try{
 data=JSON.parse(fixture('create'));
 const adminPassword=process.env.KTX_ADMIN_PASSWORD||'123';
 await login(admin,'admin',data.adminUsername,adminPassword);
 await login(student,'student',prefix+'01',data.password);
 for(const key of ['home','students','zones','rooms','registrations','contracts','invoices','payments','repairs','notifications','reports','profile','password']){
  const response=await page(admin,'/admin/'+key);assert.ok(!response.html.includes('Sắp có'),'No placeholder '+key);
 }
 await page(admin,'/admin');await page(admin,'/admin/');
 assert.equal((await student.request('/admin/students')).status,403);
 assert.equal((await student.request('/admin/reports?export=csv')).status,403);
 assert.equal((await admin.request('/admin/students',{action:'delete',id:prefix+'01'})).status,403);
 assert.equal((await admin.request('/admin/unknown')).status,404);
 pass('All admin routes render; role checks, CSRF and unknown paths work');
 const record={id:prefix+'99',name:'Sinh viên <script>QA</script>',birthday:'2005-01-01',gender:'Nam',className:'QA26',faculty:'Khoa QA',email:prefix.toLowerCase()+'@example.test'};
 await post(admin,'/admin/students',{action:'create',...record});
 let response=await page(admin,'/admin/students?q='+record.id);
 assert.ok(response.html.includes('&lt;script&gt;QA&lt;/script&gt;'));
 response=await post(admin,'/admin/students',{action:'create',...record},400);assert.ok(response.html.includes(record.email),'Invalid form keeps values');
 await post(admin,'/admin/students',{action:'update',...record,name:'Sinh viên đã sửa',faculty:'Khoa đã sửa'});
 assert.ok((await page(admin,'/admin/students?q='+record.id)).html.includes('Sinh viên đã sửa'));
 await post(admin,'/admin/students',{action:'update',...record,birthday:'2999-01-01'},400);
 await post(admin,'/admin/students',{action:'create',...record,id:'TOOLONG123456'},400);
 await post(admin,'/admin/students',{action:'delete',id:record.id});
 assert.ok(!(await page(admin,'/admin/students?q='+record.id)).html.includes('Sinh viên đã sửa'));
 await post(admin,'/admin/students',{action:'delete',id:prefix+'01'},400);
 await page(admin,'/admin/students?page=2');await page(admin,'/admin/rooms?page=2&zone=A');
 pass('Create, edit, validate, search, escape and delete roster; used history cannot be deleted');
 await post(admin,'/admin/students',{action:'accountState',id:prefix+'01',active:'0'});
 assert.equal((await student.request('/student/profile')).status,302,'Locked account loses existing session');
 await post(admin,'/admin/students',{action:'accountState',id:prefix+'01',active:'1'});
 await login(student,'student',prefix+'01',data.password);
 await post(admin,'/admin/students',{action:'rosterState',id:prefix+'01',active:'0'});
 assert.equal((await student.request('/student/profile')).status,302,'Disabled roster loses existing session');
 await post(admin,'/admin/students',{action:'rosterState',id:prefix+'01',active:'1'});
 await login(student,'student',prefix+'01',data.password);
 const roster={...record,id:prefix+'01',name:'QA student',email:prefix+'01@example.test'};
 await post(admin,'/admin/students',{action:'update',...roster,gender:'Nữ'},400);
 pass('Account and roster locks revoke sessions; gender cannot conflict with current contract');
 const other=new Client();await login(other,'student',prefix+'02',data.password);
 await post(admin,'/admin/rooms',{action:'update',id:data.room,roomStatus:'BAO_TRI'});
 await post(other,'/student/register',{action:'submit',room:data.room},400);
 await post(admin,'/admin/rooms',{action:'update',id:data.room,roomStatus:'HOAT_DONG'});
 await post(admin,'/admin/rooms',{action:'update',id:data.room,roomStatus:'INVALID'},400);
 const zoneName='Khu QA '+prefix;
 await post(admin,'/admin/zones',{action:'update',id:data.zone,name:zoneName,active:'0'});
 assert.ok((await page(admin,'/admin/zones')).html.includes(zoneName));
 await post(other,'/student/register',{action:'submit',room:data.room},400);
 await post(admin,'/admin/zones',{action:'update',id:data.zone,name:data.zoneName,active:'1'});
 await post(admin,'/admin/zones',{action:'update',id:'Z',name:'Invalid',active:'1'},404);
 assert.equal((await admin.request('/admin/rooms?id=Z999')).status,404);
 assert.equal((await admin.request('/admin/rooms?floor='+encodeURIComponent('<script>floor</script>'))).status,400);
 pass('Room maintenance and zone suspension block new bookings while contracts remain visible');
 const now=new Date().toLocaleDateString('en-CA',{timeZone:'Asia/Ho_Chi_Minh'}),[year,month]=now.split('-');
 const expectedBilled=Number(data.reportBilled)+775000,expectedPending=Number(data.reportPending)+100000;
 response=await page(admin,'/admin/reports?zone='+data.zone+'&month='+Number(month)+'&year='+year);
 assert.ok(response.html.includes(new Intl.NumberFormat('vi-VN').format(expectedBilled)),'Invoice total aggregates');assert.ok(response.html.includes(new Intl.NumberFormat('vi-VN').format(expectedPending)),'Pending payments aggregate');
 const csv=await page(admin,'/admin/reports?zone='+data.zone+'&month='+Number(month)+'&year='+year+'&export=csv');
 assert.ok(csv.headers.get('content-type').includes('text/csv'));assert.ok(csv.headers.get('content-disposition').includes('attachment'));
 assert.ok(csv.html.includes('"'+expectedBilled.toFixed(2)+'"')||csv.html.includes('"'+expectedBilled+'"'));assert.ok(csv.html.includes('"'+expectedPending.toFixed(2)+'"')||csv.html.includes('"'+expectedPending+'"'));
 assert.equal(csv.html.trim().split('\r\n').length,2,'CSV respects zone filter');
 assert.equal((await admin.request('/admin/reports?month=13&year='+year)).status,400);
 assert.equal((await admin.request('/admin/reports?month=1')).status,400);
 const reflected=await admin.request('/admin/reports?month='+encodeURIComponent('<script>report</script>')+'&year='+year);
 assert.equal(reflected.status,400);assert.ok(!reflected.html.includes('<script>report</script>'),'Invalid period is escaped');
 if(process.env.KTX_VISUAL_CHECK==='true')await checkAdminLayout(base,admin.cookies.get('JSESSIONID'),data.room,prefix+'01');
 pass('Reports and CSV reconcile invoice/pending sums, keep filters and reject invalid periods');
 const nextPassword='Admin-new-2026!';
 await post(admin,'/admin/password',{action:'change',currentPassword:'wrong',newPassword:nextPassword,confirmPassword:nextPassword},400);
 await post(admin,'/admin/password',{action:'change',currentPassword:adminPassword,newPassword:nextPassword,confirmPassword:'mismatch'},400);
 restoreAdminPassword=true;
 await post(admin,'/admin/password',{action:'change',currentPassword:adminPassword,newPassword:nextPassword,confirmPassword:nextPassword});
 assert.equal((await admin.request('/admin/profile')).status,302);
 await login(admin,'admin',data.adminUsername,nextPassword);
 pass('Admin password verifies current secret, checks confirmation and requires re-login');
 console.log('ADMIN_MANAGEMENT_CHECKS='+checks);
}finally{
 fixture('cleanup');console.log('Scoped '+prefix+' fixtures removed; original room and zone restored.');
}
