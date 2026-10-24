import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import {mkdtemp,readFile,writeFile,mkdir} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join,resolve} from 'node:path';

export async function checkAdminLayout(base,session,room,student){
 const profile=await mkdtemp(join(tmpdir(),'ktx-admin-layout-'));
 const chrome=spawn('C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=0','--user-data-dir='+profile,'about:blank'],{windowsHide:true,stdio:'ignore'});
 const pause=milliseconds=>new Promise(done=>setTimeout(done,milliseconds));
 let socket;
 try{
  let port;
  for(let attempt=0;attempt<100;attempt++){try{port=(await readFile(join(profile,'DevToolsActivePort'),'utf8')).split('\n')[0];break;}catch{await pause(100);}}
  assert.ok(port,'Chrome starts');
  const targets=await(await fetch('http://127.0.0.1:'+port+'/json')).json();
  socket=new WebSocket(targets.find(target=>target.type==='page').webSocketDebuggerUrl);
  await new Promise((done,fail)=>{socket.addEventListener('open',done,{once:true});socket.addEventListener('error',fail,{once:true});});
  let sequence=0;const pending=new Map(),errors=[];
  socket.addEventListener('message',event=>{
   const response=JSON.parse(event.data);
   if(response.method==='Runtime.exceptionThrown')errors.push(response.params.exceptionDetails.text);
   if(response.id&&pending.has(response.id)){const {done,fail}=pending.get(response.id);pending.delete(response.id);response.error?fail(new Error(response.error.message)):done(response.result);}
  });
  const call=(method,params={})=>new Promise((done,fail)=>{const id=++sequence;pending.set(id,{done,fail});socket.send(JSON.stringify({id,method,params}));});
  const evaluate=async expression=>{const response=await call('Runtime.evaluate',{expression,returnByValue:true});assert.ok(!response.exceptionDetails,JSON.stringify(response.exceptionDetails));return response.result.value;};
  async function navigate(path){
   const url=base+path;await call('Page.navigate',{url});
   for(let attempt=0;attempt<200;attempt++){if(await evaluate('document.readyState==="complete" && location.href==='+JSON.stringify(url)))return;await pause(100);}
   throw new Error('Navigation timeout '+path);
  }
  const output=resolve('target/ui-preview/admin-complete');await mkdir(output,{recursive:true});
  async function inspect(name){
   const metrics=await evaluate('({width:innerWidth,body:document.body.scrollWidth,heading:document.querySelector(".content h1")?.textContent})');
   assert.ok(metrics.heading,name+' heading');assert.ok(metrics.body<=metrics.width+2,name+' fits screen '+JSON.stringify(metrics));
   assert.ok(!await evaluate('document.body.textContent.includes("Sắp có")'),name+' no placeholder');
   const screenshot=await call('Page.captureScreenshot',{format:'png',captureBeyondViewport:false});
   await writeFile(join(output,name+'.png'),Buffer.from(screenshot.data,'base64'));
   console.log('LAYOUT '+name+' '+JSON.stringify(metrics));
  }
  await call('Page.enable');await call('Runtime.enable');await call('Network.enable');
  await call('Network.setCookie',{name:'JSESSIONID',value:session,url:base,path:new URL(base).pathname,httpOnly:true});
  const paths=['home','students','students?action=new','students?action=edit&id='+student,'zones','zones?id='+room[0],'rooms','rooms?id='+room,'reports','profile','password','registrations','contracts','invoices','payments','repairs','notifications'];
  for(const width of [1440,390]){
   await call('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<760});
   for(const [index,path] of paths.entries()){await navigate('/admin/'+path);await inspect(width+'-'+index+'-'+path.split('?')[0]);}
  }
  await navigate('/admin/password');
  await evaluate('document.querySelector("[data-show-passwords]").click()');
  assert.equal(await evaluate('document.getElementById("currentPassword").type'),'text');
  await evaluate('document.getElementById("newPassword").value="long-password";document.getElementById("confirmPassword").value="other-password";document.getElementById("confirmPassword").dispatchEvent(new Event("input"))');
  assert.equal(await evaluate('document.getElementById("confirmPassword").checkValidity()'),false,'Password mismatch');
  await evaluate('document.querySelector("[data-nav-toggle]").click()');
  assert.equal(await evaluate('document.querySelector("[data-nav-toggle]").getAttribute("aria-expanded")'),'true');
  await evaluate('document.dispatchEvent(new KeyboardEvent("keydown",{key:"Escape",bubbles:true}))');
  assert.equal(await evaluate('document.querySelector("[data-nav-toggle]").getAttribute("aria-expanded")'),'false');
  await navigate('/admin/students?action=new');
  await evaluate('document.getElementById("student-id").value="INVALID ID"');
  assert.equal(await evaluate('document.getElementById("student-id").checkValidity()'),false,'Student code validates');
  await navigate('/admin/reports');await call('Emulation.setEmulatedMedia',{media:'print'});
  assert.equal(await evaluate('getComputedStyle(document.querySelector(".sidebar")).display'),'none');
  await inspect('reports-print');
  assert.deepEqual(errors,[],'No browser JavaScript errors');
  console.log('ADMIN_LAYOUT_CHECKS_PASS');
 }finally{socket?.close();chrome.kill();}
}
