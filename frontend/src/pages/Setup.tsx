import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, ChevronRight, Code2, Copy, ExternalLink, Globe2, Save, Settings2, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import { api, session } from "../api";

type Channel = { channelId:string; businessId:string; type:string; externalId:string; enabled:boolean; allowedOrigins:string[] };
export default function Setup() {
  const id = session.businessId()!;
  const [s,setS]=useState<any>(null);
  const [channel,setChannel]=useState<Channel|null>(null);
  const [origin,setOrigin]=useState("");
  const [origins,setOrigins]=useState<string[]>([]);
  const [busy,setBusy]=useState(false);
  const [saved,setSaved]=useState(false);
  const [error,setError]=useState("");

  useEffect(()=>{ if(!id)return; api.summary(id).then(setS).catch(e=>setError(e.message)); api.channels(id).then(channels=> {
    const website=channels.find(c=>c.type==="WEBSITE");
    if(website){ setChannel(website as Channel); setOrigins(website.allowedOrigins ?? []); }
  }).catch(e=>setError(e.message)); },[id]);

  const widgetCode=useMemo(()=>channel
    ? `<script
  src="${window.location.origin}/inquiro-widget.js"
  data-channel-id="${channel.channelId}">
</script>`
    : "",[channel]);

  async function saveOrigins(){
    if(!channel)return;
    setBusy(true);setError("");setSaved(false);
    try{const next=origin.trim().replace(/\/$/,"");const merged=next && !origins.includes(next)?[...origins,next]:origins;
      const r=await api.saveWebsiteConfig(id,channel.channelId,merged);setOrigins(r.allowedOrigins);setOrigin("");setSaved(true);
      setChannel({...channel,allowedOrigins:r.allowedOrigins});
    }catch(e){setError(e instanceof Error?e.message:"Unable to save website settings");}finally{setBusy(false);}
  }
  async function removeOrigin(value:string){
    if(!channel)return;
    setBusy(true);setError("");
    try{const r=await api.saveWebsiteConfig(id,channel.channelId,origins.filter(x=>x!==value));setOrigins(r.allowedOrigins);setChannel({...channel,allowedOrigins:r.allowedOrigins});}
    catch(e){setError(e instanceof Error?e.message:"Unable to save website settings");}finally{setBusy(false);}
  }
  async function copyCode(){await navigator.clipboard?.writeText(widgetCode);setSaved(true);setTimeout(()=>setSaved(false),1800);}
  const testUrl=channel ? `/widget?channelId=${encodeURIComponent(channel.channelId)}` : "/widget";
  const ready=Boolean(channel?.enabled);

  const items=[
    ["Business information","Your identity, industry and description",s?.businessInformationComplete,"/onboarding"],
    ["Services","Choose the workflows your AI can handle",s?.servicesConfigured,"/onboarding"],
    ["Knowledge","Give the AI accurate business context",s?.knowledgeConfigured,"/onboarding"],
    ["Website chat","Install and secure your website receptionist",ready,"#website"]
  ];

  return <div className="dashboard">
    <div className="page-top"><div><span className="eyebrow">Configuration</span><h1>Business setup</h1><p className="muted">Control what Inquiro knows, does and where it talks to customers.</p></div></div>
    <div className="setup-hero"><div className="setup-icon"><Settings2/></div><div><h2>Workspace readiness</h2><p>{s?.readyForReceptionist?"Everything required for the receptionist is configured.":"Complete the remaining items before going live."}</p></div><span className={`readiness ${s?.readyForReceptionist?"ready":""}`}>{s?.readyForReceptionist?"Ready":"In progress"}</span></div>
    <div className="setup-list">{items.map(([title,desc,ok,to])=><Link to={String(to)} className="setup-row" key={String(title)}><span className={ok?"setup-check done":"setup-check"}>{ok&&<CheckCircle2 size={19}/>}</span><div><b>{String(title)}</b><small>{String(desc)}</small></div><span className="setup-state">{ok?"Complete":"Configure"}</span><ChevronRight size={18}/></Link>)}</div>

    <section id="website" className="website-install-panel">
      <div className="website-install-heading"><div className="setup-icon"><Globe2/></div><div><span className="eyebrow">Website receptionist</span><h2>Install Inquiro on your website</h2><p className="muted">Add one small script to your website. The widget opens in a secure hosted chat window.</p></div></div>
      {!channel ? <div className="website-empty"><Globe2 size={28}/><b>No website channel yet</b><p>Complete onboarding and enable Website Chat first.</p><Link className="primary-button compact" to="/onboarding">Enable website chat</Link></div> :
      <div className="website-install-grid">
        <div className="install-card"><div className="install-card-title"><Code2 size={17}/><b>1. Copy the installation code</b></div><pre>{widgetCode}</pre><div className="install-actions"><button className="primary-button" onClick={copyCode}><Copy size={15}/>{saved?"Copied":"Copy code"}</button><a className="secondary-button" href={testUrl} target="_blank" rel="noreferrer"><ExternalLink size={15}/>Test widget</a></div><small className="install-note">Paste it before the closing &lt;/body&gt; tag, or use your website platform's custom HTML/code injection area.</small></div>
        <div className="install-card"><div className="install-card-title"><Globe2 size={17}/><b>2. Secure your website</b></div><p className="install-note">Add the exact website origin where the widget will be installed. Inquiro will reject public chat requests from other origins once you configure an allowlist.</p><div className="origin-add"><input value={origin} onChange={e=>setOrigin(e.target.value)} placeholder="https://www.example.com" onKeyDown={e=>{if(e.key==="Enter"){e.preventDefault();void saveOrigins()}}}/><button className="primary-button" disabled={!origin.trim()||busy} onClick={()=>void saveOrigins()}><Save size={15}/>Add</button></div><div className="origin-list">{origins.length===0?<div className="origin-empty">No domains configured yet. Add your production website before launch.</div>:origins.map(o=><div className="origin-row" key={o}><span>{o}</span><button onClick={()=>void removeOrigin(o)} disabled={busy}>Remove</button></div>)}</div></div>
      </div>}
      {error&&<div className="error-box">{error}</div>}
    </section>
    <div className="tip"><Sparkles size={17}/><span>For WordPress, Shopify, Wix or another site builder, use its custom HTML/code injection feature. The business owner does not need to build the chat interface.</span></div>
  </div>;
}