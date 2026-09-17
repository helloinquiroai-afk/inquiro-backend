const state={sessionId:localStorage.getItem('inquiro.demo.session')||crypto.randomUUID()};
const messages=document.getElementById('messages');
const form=document.getElementById('chatForm');
const input=document.getElementById('message');
const send=document.getElementById('send');
const error=document.getElementById('error');
const missing=document.getElementById('missingLabel');
const stateLabel=document.getElementById('stateLabel');
const sessionLabel=document.getElementById('sessionLabel');

sessionLabel.textContent=state.sessionId.slice(0,8)+'…';
localStorage.setItem('inquiro.demo.session',state.sessionId);

function addMessage(text,role){
  const el=document.createElement('div');
  el.className=`message ${role}`;
  el.textContent=text||'';
  messages.appendChild(el);
  messages.scrollTop=messages.scrollHeight;
}
function setBusy(busy){send.disabled=busy;input.disabled=busy;send.textContent=busy?'…':'Send'}

form.addEventListener('submit',async e=>{
  e.preventDefault();
  const text=input.value.trim();
  if(!text)return;
  error.textContent='';
  addMessage(text,'user');
  input.value='';setBusy(true);
  try{
    const response=await fetch('/api/conversations/message',{
      method:'POST',headers:{'Content-Type':'application/json'},
      body:JSON.stringify({sessionId:state.sessionId,message:text})
    });
    const data=await response.json().catch(()=>({}));
    if(!response.ok)throw new Error(data.message||'The receptionist could not process that message.');
    addMessage(data.reply||'Thank you. How else can I help?','bot');
    const fields=data.missingFields||[];
    missing.textContent=fields.length?fields.join(', '):'—';
    stateLabel.textContent=data.status||'Active';
    if(data.bookingId||data.booking){stateLabel.textContent='Booking confirmed'}
  }catch(err){error.textContent=err.message||'Something went wrong.'}
  finally{setBusy(false);input.focus()}
});

document.getElementById('newChat').addEventListener('click',()=>{
  state.sessionId=crypto.randomUUID();
  localStorage.setItem('inquiro.demo.session',state.sessionId);
  sessionLabel.textContent=state.sessionId.slice(0,8)+'…';
  messages.innerHTML='<div class="message bot">Hi! Welcome. How can I help you today?</div>';
  missing.textContent='—';stateLabel.textContent='Ready';error.textContent='';input.focus();
});

document.getElementById('clearChat').addEventListener('click',async()=>{
  try{await fetch('/api/conversations/'+encodeURIComponent(state.sessionId),{method:'DELETE'});}catch(_){ }
  document.getElementById('newChat').click();
});
