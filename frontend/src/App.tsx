import { Navigate, Route, Routes } from "react-router-dom";
import { useState } from "react";
import { session } from "./api";
import Auth from "./pages/Auth";
import Onboarding from "./pages/Onboarding";
import Dashboard from "./pages/Dashboard";
import Bookings from "./pages/Bookings";
import Setup from "./pages/Setup";
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
    <Route path="/bookings" element={<Protected><Layout><Bookings/></Layout></Protected>} />
    <Route path="/setup" element={<Protected><Layout><Setup/></Layout></Protected>} />
    <Route path="/" element={<Protected><Layout><Dashboard/></Layout></Protected>} />
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>;
}