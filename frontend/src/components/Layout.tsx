import type { ReactNode } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { Bot, CalendarDays, LayoutDashboard, LogOut, Settings2, Sparkles } from "lucide-react";
import { api, session } from "../api";
export default function Layout({children}:{children:ReactNode}) {
  const navigate=useNavigate();
  const logout=async()=>{try{await api.logout()}catch{} session.clear(); sessionStorage.removeItem("inquiro.businessId"); navigate("/login");};
  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark"><Sparkles size={18}/></div><div><strong>inquiro</strong><span>AI receptionist</span></div></div>
      <div className="business-pill"><span className="status-dot"/><div><b>Business workspace</b><small>{session.businessId() ?? "Setup required"}</small></div></div>
      <nav>
        <NavLink to="/" end><LayoutDashboard size={18}/>Overview</NavLink>
        <NavLink to="/bookings"><CalendarDays size={18}/>Bookings</NavLink>
        <NavLink to="/setup"><Settings2 size={18}/>Business setup</NavLink>
      </nav>
      <div className="sidebar-bottom"><div className="ai-card"><Bot size={19}/><b>Your AI is ready</b><small>Configure your business and connect a channel to go live.</small></div><button className="ghost-button" onClick={logout}><LogOut size={17}/>Sign out</button></div>
    </aside>
    <main className="main">{children}</main>
  </div>;
}