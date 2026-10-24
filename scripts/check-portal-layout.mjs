import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import {mkdtemp,readFile,writeFile,mkdir} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join,resolve} from 'node:path';

export async function checkPortalLayout(base,studentSession,adminSession,invoice){
 const profile=await mkdtemp(join(tmpdir(),'ktx-compact-layout-'));
 const chrome=spawn('C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=0','--user-data-dir='+profile,'about:blank'],{windowsHide:true,stdio:'ignore'});
 const pause=ms=>new Promise(done=>setTimeout(done,ms));let ws;
 try{
  let port;for(let i=0;i<100;i++){try{port=(await readFile(join(profile,'DevToolsActivePort'),'utf8')).split('\n')[0];break;}catch{await pause(100);}}
  assert.ok(port,'Browser starts');
  const targets=await(await fetch('http://127.0.0.1:'+port+'/json')).json();
  ws=new WebSocket(targets.find(p=>p.type==='page').webSocketDebuggerUrl);
  await new Promise((done,fail)=>{ws.addEventListener('open',done,{once:true});ws.addEventListener('error',fail,{once:true});});
  let sequence=0;const pending=new Map();
  ws.addEventListener('message',event=>{const r=JSON.parse(event.data);if(r.id&&pending.has(r.id)){const {done,fail}=pending.get(r.id);pending.delete(r.id);r.error?fail(new Error(r.error.message)):done(r.result);}});
  const call=(method,params={})=>new Promise((done,fail)=>{const id=++sequence;pending.set(id,{done,fail});ws.send(JSON.stringify({id,method,params}));});
  const evaluate=async expression=>{const r=await call('Runtime.evaluate',{expression,returnByValue:true});assert.ok(!r.exceptionDetails,'Browser evaluates');return r.result.value;};
  async function navigate(path){
   const url=base+path;await call('Page.navigate',{url});
   for(let i=0;i<200;i++){if(await evaluate('document.readyState==="complete" && location.href==='+JSON.stringify(url)))return;await pause(100);}
   throw new Error('Navigation timeout '+path);
  }
  await mkdir(resolve('target/ui-preview/compact'),{recursive:true});
  async function inspect(name){
   const metrics=await evaluate('({width:innerWidth,body:document.body.scrollWidth,heading:document.querySelector("h1")?.textContent,rooms:document.querySelectorAll(".portal-room-grid .room-card").length})');
   assert.ok(metrics.body<=metrics.width+2,name+' fits viewport');assert.ok(metrics.heading,name+' has heading');
   assert.ok(!await evaluate('['+JSON.stringify(['�','áº','á»','Ã¡','Ä‘','Â·'])+'][0].some(marker=>document.body.textContent.includes(marker))'),name+' has valid Vietnamese text');
   const picture=await call('Page.captureScreenshot',{format:'png',captureBeyondViewport:false});
   await writeFile(resolve('target/ui-preview/compact',name+'.png'),Buffer.from(picture.data,'base64'));
   console.log('LAYOUT '+name+' '+JSON.stringify(metrics));
  }
  const session=value=>call('Network.setCookie',{name:'JSESSIONID',value,url:base,path:'/QuanLyKyTucXa',httpOnly:true});
  const size=width=>call('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<760});
  await call('Page.enable');await call('Network.enable');await session(studentSession);await size(1440);
  for(const path of ['profile','rooms','payments?invoice='+invoice,'invoices?id='+invoice]){await navigate('/student/'+path);await inspect('student-'+path.split('?')[0]+'-desktop');}
  await navigate('/student/payments?invoice='+invoice);
  assert.ok(await evaluate('!!document.querySelector("form[method=post]")'),'Payment form available');
  assert.equal(await evaluate('document.querySelector(".portal-optional").open'),false,'Optional reference collapsed');
  await size(390);await inspect('student-payments-mobile');
  for(const path of ['profile','rooms','history','invoices?id='+invoice]){await navigate('/student/'+path);await inspect('student-'+path.split('?')[0]+'-mobile');}
  await session(adminSession);await size(1440);
  for(const path of ['registrations','payments','invoices','home']){await navigate('/admin/'+path);await inspect('admin-'+path+'-desktop');}
  await size(390);await navigate('/admin/registrations');await inspect('admin-registrations-mobile');
 }finally{if(ws)ws.close();chrome.kill();}
}
