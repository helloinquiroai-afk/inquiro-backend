import type { FormEvent } from "react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowRight, Bot, LockKeyhole, Mail, Sparkles, User } from "lucide-react";
import { api, session } from "../api";
export default function Auth({mode,onAuth}:{mode:"login"|"register";onAuth:()=>void}) {
 const register=mode==="register"; const [name,setName]=useState(""); const [email,setEmail]=useState(""); const [password,setPassword]=useState(""); const [busy,setBusy]=useState(false); const [error,setError]=useState("");
 const nav=useNavigate();
 async function submit(e:FormEvent){e.preventDefault();setError("");setBusy(true);try{if(register) await api.register(name,email,password); const r=await api.login(email,password);session.set(r.accessToken);nav("/onboarding");onAuth();}catch(err){setError(err instanceof Error?err.message:"Unable to continue");}finally{setBusy(false);}}
 return <div className="auth-page"><div className="auth-visual"><div className="auth-glow"/><div className="brand light"><div className="brand-mark"><Sparkles size={18}/></div><div><strong>inquiro</strong><span>AI receptionist</span></div></div><div className="visual-copy"><span className="eyebrow">Customer operations, simplified</span><h1>Let your business<br/><em>answer itself.</em></h1><p>Turn customer questions into helpful answers, qualified requests and real bookings — around the clock.</p><div className="mini-proof"><Bot size={17}/><span>Built for hotels, restaurants and clinics.</span></div></div></div>
 <div className="auth-panel"><div className="auth-inner"><div className="mobile-brand brand"><div className="brand-mark"><Sparkles size={18}/></div><strong>inquiro</strong></div><span className="eyebrow">{register?"Create your workspace":"Welcome back"}</span><h2>{register?"Start your AI receptionist":"Sign in to Inquiro"}</h2><p className="muted">{register?"Create an account and configure your first business in minutes.":"Continue managing your business, bookings and AI receptionist."}</p><form onSubmit={submit}>
 {register&&<label><span>Name</span><div className="input-wrap"><User size={17}/><input value={name} onChange={e=>setName(e.target.value)} required maxLength={200} placeholder="Your name"/></div></label>}
 <label><span>Email</span><div className="input-wrap"><Mail size={17}/><input type="email" value={email} onChange={e=>setEmail(e.target.value)} required placeholder="you@company.com"/></div></label>
 <label><span>Password</span><div className="input-wrap"><LockKeyhole size={17}/><input type="password" value={password} onChange={e=>setPassword(e.target.value)} required minLength={12} placeholder="At least 12 characters"/></div></label>
 {error&&<div className="error-box">{error}</div>}<button className="primary-button" disabled={busy}>{busy?"Please wait…":register?"Create account":"Sign in"}<ArrowRight size={17}/></button></form>
 <p className="auth-switch">{register?"Already have an account? ":"New to Inquiro? "}<Link to={register?"/login":"/register"}>{register?"Sign in":"Create an account"}</Link></p></div></div></div>;
}