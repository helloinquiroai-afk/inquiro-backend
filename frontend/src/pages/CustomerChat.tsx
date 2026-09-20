import { useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent } from "react";
import { Bot, Check, RotateCcw, Send, Sparkles, UserRound } from "lucide-react";
import { Link } from "react-router-dom";
import { api, session } from "../api";

type Message = { id:string; role:"customer"|"assistant"; text:string; bookingId?:string };

function createSessionId(){
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) return crypto.randomUUID();
  return `chat-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export default function CustomerChat(){
  const businessId=session.businessId() ?? "";
  const storageKey=`inquiro.chat.sessionId.${businessId}`;
  const [chatSessionId,setChatSessionId]=useState(()=>sessionStorage.getItem(storageKey) ?? createSessionId());
  const [channelId,setChannelId]=useState("");
  const [loadingChannel,setLoadingChannel]=useState(true);
  const [messages,setMessages]=useState<Message[]>([]);
  const [draft,setDraft]=useState("");
  const [sending,setSending]=useState(false);
  const [error,setError]=useState("");
  const endRef=useRef<HTMLDivElement>(null);

  useEffect(()=>{sessionStorage.setItem(storageKey,chatSessionId)},[storageKey,chatSessionId]);
  useEffect(()=>{endRef.current?.scrollIntoView({behavior:"smooth"})},[messages,sending]);
  useEffect(()=>{
    let active=true;
    if(!businessId){setLoadingChannel(false);return}
    api.channels(businessId).then(channels=>{
      if(!active)return;
      const website=channels.find(c=>c.type==="WEBSITE"&&c.enabled);
      if(website)setChannelId(website.externalId);
    }).catch(e=>{if(active)setError(e instanceof Error?e.message:"Unable to load customer channel")}).finally(()=>{if(active)setLoadingChannel(false)});
    return()=>{active=false};
  },[businessId]);

  const canSend=useMemo(()=>Boolean(channelId&&!loadingChannel&&!sending&&draft.trim()),[channelId,loadingChannel,sending,draft]);

  async function send(e?:FormEvent){
    e?.preventDefault();
    const text=draft.trim();
    if(!canSend)return;
    setDraft("");setError("");setSending(true);
    const userMessage:Message={id:createSessionId(),role:"customer",text};
    setMessages(current=>[...current,userMessage]);
    try{
      const response=await api.conversation(chatSessionId,text,channelId);
      setMessages(current=>[...current,{id:createSessionId(),role:"assistant",text:response.reply || "I’m here to help.",bookingId:response.bookingId}]);
    }catch(err){
      setError(err instanceof Error?err.message:"Unable to send message");
    }finally{setSending(false)}
  }

  async function reset(){
    setError("");
    try{if(channelId)await api.clearConversation(chatSessionId,channelId)}catch(err){setError(err instanceof Error?err.message:"Unable to reset conversation");return}
    const next=createSessionId();
    setChatSessionId(next);
    setMessages([]);
  }

  return <div className="chat-page">
    <div className="page-top">
      <div><span className="eyebrow"><Sparkles size={13}/>Customer experience</span><h1>Customer chat</h1><p className="muted">Preview the same AI receptionist your website visitors will use.</p></div>
      <div className="chat-page-actions"><button className="secondary-button" onClick={reset} disabled={!channelId||sending}><RotateCcw size={15}/>New conversation</button><Link className="secondary-button" to="/setup">Business setup</Link></div>
    </div>

    <div className="chat-preview-grid">
      <section className="chat-preview-card">
        <div className="chat-header"><div className="chat-brand"><div className="chat-brand-icon"><Bot size={19}/></div><div><strong>Inquiro AI Receptionist</strong><span>Online · Website chat</span></div></div><span className="chat-online"><span/>Online</span></div>
        <div className="chat-messages">
          {messages.length===0&&<div className="chat-empty"><div className="chat-welcome-icon"><Bot size={28}/></div><h2>How can we help?</h2><p>Ask a question or start a booking request.</p><div className="quick-prompts"><button onClick={()=>setDraft("What are your check-in and check-out times?")}>Check-in times</button><button onClick={()=>setDraft("I want to book a room")}>Book a room</button><button onClick={()=>setDraft("Do you offer airport pickup?")}>Airport pickup</button></div></div>}
          {messages.map(message=><div className={`chat-message-row ${message.role}`} key={message.id}><div className="chat-avatar">{message.role==="assistant"?<Bot size={16}/>:<UserRound size={16}/>}</div><div><div className="chat-bubble">{message.text}</div>{message.bookingId&&<div className="booking-confirmation"><Check size={14}/><span>Booking reference: <strong>{message.bookingId}</strong></span></div>}</div></div>)}
          {sending&&<div className="chat-message-row assistant"><div className="chat-avatar"><Bot size={16}/></div><div className="typing"><span/><span/><span/></div></div>}
          {loadingChannel&&<div className="chat-loading">Connecting to your website channel…</div>}
          {!loadingChannel&&!channelId&&<div className="chat-channel-warning">No website chat channel is connected. <Link to="/onboarding">Enable website chat</Link> to test the receptionist.</div>}
          <div ref={endRef}/>
        </div>
        <form className="chat-composer" onSubmit={send}>
          <textarea value={draft} onChange={e=>setDraft(e.target.value)} onKeyDown={e=>{if(e.key==="Enter"&&!e.shiftKey){e.preventDefault();void send()}}} placeholder="Type a customer message…" rows={1} disabled={!channelId||sending}/>
          <button className="primary-button chat-send" type="submit" disabled={!canSend} aria-label="Send message"><Send size={17}/></button>
        </form>
        {error&&<div className="error-box chat-error">{error}</div>}
      </section>

      <aside className="chat-info">
        <div className="panel chat-info-card"><div className="panel-title"><div><h3>What you're testing</h3><p>This preview uses your live workspace configuration.</p></div></div><div className="chat-check-list"><div><Check size={15}/><span>Business knowledge</span></div><div><Check size={15}/><span>Configured booking workflows</span></div><div><Check size={15}/><span>Conversation memory</span></div><div><Check size={15}/><span>Website channel</span></div></div></div>
        <div className="chat-tip"><Sparkles size={17}/><div><strong>Try a booking</strong><p>For your hotel demo, start with “I want to book a room” and answer Inquiro’s follow-up questions.</p></div></div>
      </aside>
    </div>
  </div>
}