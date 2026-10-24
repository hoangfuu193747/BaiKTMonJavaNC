import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {resolve} from 'node:path';
import {checkPortalLayout} from './check-portal-layout.mjs';
const base='http://127.0.0.1:18080/QuanLyKyTucXa',prefix='PT'+randomBytes(3).toString('hex').toUpperCase();
function fixture(action){
 const args=['-NoProfile','-ExecutionPolicy','Bypass','-File',resolve('scripts/portal-fixture.ps1'),'-Action',action,'-Prefix',prefix];
 if(action==='create')args.push('-ForVisual');
 const r=spawnSync('powershell.exe',args,{encoding:'utf8',windowsHide:true});if(r.status!==0)throw new Error(r.stderr);return r.stdout.trim();
}
async function login(role,name,password){
 const page=await fetch(base+'/auth/login?role='+role);assert.equal(page.status,200);
 const cookie=page.headers.getSetCookie().find(c=>c.startsWith('JSESSIONID=')).split(';')[0];
 const html=await page.text(),csrfToken=html.match(/name="csrfToken" value="([^"]+)"/)[1];
 const response=await fetch(base+'/auth/login',{method:'POST',redirect:'manual',headers:{Cookie:cookie,'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({csrfToken,role,username:name,password}),signal:AbortSignal.timeout(20000)});
 assert.equal(response.status,303,'Login '+role);
 return response.headers.getSetCookie().find(c=>c.startsWith('JSESSIONID=')).split(';')[0].slice('JSESSIONID='.length);
}
try{
 const data=JSON.parse(fixture('create'));
 const admin=await login('admin','admin',process.env.KTX_ADMIN_PASSWORD||'123');
 const student=await login('student',prefix+'01',data.password);
 await checkPortalLayout(base,student,admin,data.invoice);
 console.log('LAYOUT_AND_VIETNAMESE_TEXT_PASS');
}finally{fixture('cleanup');console.log('Scoped visual fixtures removed: '+prefix);}
