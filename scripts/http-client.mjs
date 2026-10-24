export const base=(process.env.KTX_WEB_URL||'http://127.0.0.1:18080/QuanLyKyTucXa').replace(/\/$/,'');

export class Client {
 cookies=new Map();
 async request(path,form){
  const method=form?'POST':'GET';
  if(process.env.PORTAL_TRACE)console.log(method+' '+path);
  const headers={Cookie:[...this.cookies].map(([name,value])=>name+'='+value).join('; ')};
  if(form)headers['Content-Type']='application/x-www-form-urlencoded';
  let response;
  try{
   response=await fetch(base+path,{method,headers,body:form?new URLSearchParams(form):undefined,
    redirect:'manual',signal:AbortSignal.timeout(30000)});
  }catch(error){throw new Error(method+' '+path+': '+error.message);}
  for(const cookie of response.headers.getSetCookie()){
   const [name,...value]=cookie.split(';')[0].split('=');
   this.cookies.set(name,value.join('='));
  }
  return {status:response.status,location:response.headers.get('location'),headers:response.headers,html:await response.text()};
 }
}
