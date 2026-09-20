import { Navigate, Route, Routes } from "react-router-dom";
import { useState } from "react";
import { session } from "./api";
import Auth from "./pages/Auth";
import Onboarding from "./pages/Onboarding";
import Dashboard from "./pages/Dashboard";
import Layout from "./components/Layout";

function Protected({children}:{children:React.ReactNode}) {
  return session.get() ? <>{children}</> : <Navigate to="/login" replace />;
}
export default function App() {
  const [refresh,setRefresh] = useState(0);
  void refresh;
  return <Routes>
    <Route path="/login" element={<Auth mode="login" onAuth={()=>setRefresh(v=>v+1)}/>} />
    <Route path="/register" element={<Auth mode="register" onAuth={()=>setRefresh(v=>v+1)}/>} />
    <Route path="/onboarding/*" element={<Protected><Layout><Onboarding/></Layout></Protected>} />
    <Route path="/*" element={<Protected><Layout><Dashboard/></Layout></Protected>} />
  </Routes>;
}