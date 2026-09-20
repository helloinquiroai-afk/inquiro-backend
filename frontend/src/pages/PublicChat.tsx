import { useMemo, useState } from "react";
import type { FormEvent } from "react";
import { Bot, Send, Sparkles, UserRound } from "lucide-react";
import { api } from "../api";
import type { ConversationResponse } from "../types";

type Message = { role: "assistant" | "user"; text: string };

export default function PublicChat() {
  const params = new URLSearchParams(window.location.search);
  const channelId = params.get("channelId")?.trim() ?? "";
  const siteOrigin = params.get("siteOrigin")?.trim() || window.location.origin;
  const storageKey = useMemo(() => `inquiro.public.chat.${channelId}`, [channelId]);
  const [sessionId] = useState(() => sessionStorage.getItem(storageKey) ?? crypto.randomUUID());
  const [messages, setMessages] = useState<Message[]>([]);
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  if (!channelId) {
    return <div className="public-chat-shell"><div className="public-chat-card"><h2>Chat is not configured</h2><p>This widget needs a website channel ID.</p></div></div>;
  }

  async function send(event?: FormEvent) {
    event?.preventDefault();
    const message = text.trim();
    if (!message || busy) return;
    setText("");
    setError("");
    setMessages(current => [...current, { role: "user", text: message }]);
    setBusy(true);
    sessionStorage.setItem(storageKey, sessionId);
    try {
      const response: ConversationResponse = await api.publicConversation(sessionId, message, channelId, siteOrigin);
      setMessages(current => [...current, { role: "assistant", text: response.reply || "Thanks. How else can I help?" }]);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to send message");
    } finally {
      setBusy(false);
    }
  }

  return <div className="public-chat-shell">
    <div className="public-chat-card">
      <header className="public-chat-header">
        <div className="public-chat-brand"><span><Sparkles size={17}/></span><div><strong>Inquiro</strong><small>AI receptionist</small></div></div>
        <span className="public-chat-online"><i/>Online</span>
      </header>
      <div className="public-chat-messages">
        {messages.length === 0 && <div className="public-chat-welcome"><span><Bot size={25}/></span><h2>How can we help?</h2><p>Ask a question or tell us what you would like to book.</p><div><button onClick={() => setText("I would like to make a booking.")}>Make a booking</button><button onClick={() => setText("What services do you offer?")}>Our services</button></div></div>}
        {messages.map((message, index) => <div className={`public-message ${message.role}`} key={index}><span className="public-avatar">{message.role === "user" ? <UserRound size={15}/> : <Bot size={15}/>}</span><div>{message.text}</div></div>)}
        {busy && <div className="public-message assistant"><span className="public-avatar"><Bot size={15}/></span><div className="public-typing"><i/><i/><i/></div></div>}
      </div>
      {error && <div className="public-chat-error">{error}</div>}
      <form className="public-chat-composer" onSubmit={send}><textarea value={text} onChange={e => setText(e.target.value)} onKeyDown={e => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); void send(); } }} placeholder="Type your message…" rows={1}/><button type="submit" disabled={busy || !text.trim()} aria-label="Send"><Send size={17}/></button></form>
    </div>
  </div>;
}
