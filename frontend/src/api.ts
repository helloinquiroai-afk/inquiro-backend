import type { BusinessKnowledge, Catalog, OnboardingSummary, ServiceDefinition, Booking } from "./types";

const API = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const TOKEN_KEY = "inquiro.session";

export const session = {
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (token:string) => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY),
  businessId: () => sessionStorage.getItem("inquiro.businessId"),
  setBusinessId: (id:string) => sessionStorage.setItem("inquiro.businessId", id)
};

async function request<T>(path:string, init:RequestInit = {}):Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Content-Type","application/json");
  const token = session.get();
  if (token) headers.set("Authorization",`Bearer ${token}`);
  const res = await fetch(`${API}${path}`, {...init, headers});
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try { const body = await res.json(); message = body.message ?? message; } catch {}
    throw new Error(message);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}
export const api = {
  register: (name:string,email:string,password:string) => request<{userId:string;email:string;name:string}>("/api/auth/register",{method:"POST",body:JSON.stringify({name,email,password})}),
  login: (email:string,password:string) => request<{token:string;userId:string;email:string;name:string}>("/api/auth/login",{method:"POST",body:JSON.stringify({email,password})}),
  createBusiness: (businessName:string,businessType:string,description:string) =>
    request<{businessId:string;businessName:string}>("/api/business/accounts",{method:"POST",body:JSON.stringify({businessName,businessType,description,services:[],knowledge:{businessDescription:"",services:[],products:[],facts:{},faqs:[],policies:[],instructions:"",operatingHours:{},locations:[],contactInformation:{},bookingRules:{},capabilities:[],restrictions:[]}})}),
  summary: (id:string) => request<OnboardingSummary>(`/api/business/accounts/${id}/onboarding`),
  catalog: (id:string) => request<Catalog>(`/api/business/accounts/${id}/onboarding/catalog`),
  saveBusiness: (id:string,data:{businessName:string;businessType:string;description:string}) =>
    request<OnboardingSummary>(`/api/business/accounts/${id}/onboarding/business`,{method:"PUT",body:JSON.stringify(data)}),
  saveServices: (id:string,services:ServiceDefinition[]) =>
    request<OnboardingSummary>(`/api/business/accounts/${id}/onboarding/services`,{method:"PUT",body:JSON.stringify({services})}),
  saveKnowledge: (id:string,knowledge:BusinessKnowledge) =>
    request<OnboardingSummary>(`/api/business/accounts/${id}/onboarding/knowledge`,{method:"PUT",body:JSON.stringify({knowledge})}),
  complete: (id:string) => request<OnboardingSummary>(`/api/business/accounts/${id}/onboarding/complete`,{method:"POST"}),
  bookings: (id:string) => request<Booking[]>(`/api/business/accounts/${id}/bookings`),
  channels: (id:string) => request<{channelId:string;businessId:string;type:string;externalId:string;enabled:boolean}[]>(`/api/business/accounts/${id}/channels`),
  connectWebsite: (id:string) => request<{channelId:string;businessId:string;type:string;externalId:string;enabled:boolean}>(`/api/business/accounts/${id}/channels`,{method:"POST",body:JSON.stringify({type:"WEBSITE",externalId:`website-${id}`,enabled:true})}),
  logout: () => request<void>("/api/auth/logout",{method:"POST"})
};