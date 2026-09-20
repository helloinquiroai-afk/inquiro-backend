import { useEffect, useMemo, useState } from "react";
import { MapContainer, Marker, Popup, TileLayer, useMapEvents } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import {
  ArrowLeft, ArrowRight, Check, CheckCircle2, Clock3, Hotel, Hospital,
  MessageCircle, PartyPopper, Sparkles, Utensils, WandSparkles
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { api, session } from "../api";
import type { BusinessKnowledge, BusinessLocation, Catalog, OnboardingSummary, ServiceDefinition } from "../types";

const emptyKnowledge: BusinessKnowledge = {
  businessDescription: "", services: [], products: [], facts: {}, faqs: [], policies: [],
  instructions: "", operatingHours: {}, locations: [], locationDetails: [], contactInformation: {},
  bookingRules: {}, capabilities: [], restrictions: []
};

const icons: Record<string, typeof Hotel> = {
  HOSPITALITY: Hotel,
  RESTAURANT: Utensils,
  HEALTHCARE: Hospital,
  OTHER: Hospital
};

const FACILITY_OPTIONS: Record<string, { key: string; label: string; hint?: string }[]> = {
  HOSPITALITY: [
    { key: "petsAllowed", label: "Pets allowed" },
    { key: "smokingAllowed", label: "Smoking allowed" },
    { key: "spaAvailable", label: "Spa available" },
    { key: "poolAvailable", label: "Swimming pool" },
    { key: "gymAvailable", label: "Gym / fitness" },
    { key: "restaurantAvailable", label: "Restaurant on site" },
    { key: "breakfastAvailable", label: "Breakfast available" },
    { key: "parkingAvailable", label: "Parking available" },
    { key: "wifiAvailable", label: "Free Wi-Fi" },
    { key: "airportPickupAvailable", label: "Airport pickup" },
    { key: "familyFriendly", label: "Family friendly" },
    { key: "wheelchairAccessible", label: "Wheelchair accessible" }
  ],
  RESTAURANT: [
    { key: "reservationsAccepted", label: "Table reservations" },
    { key: "takeawayAvailable", label: "Takeaway available" },
    { key: "deliveryAvailable", label: "Delivery available" },
    { key: "outdoorSeating", label: "Outdoor seating" },
    { key: "kidsMenuAvailable", label: "Kids menu" },
    { key: "vegetarianOptions", label: "Vegetarian options" },
    { key: "halalOptions", label: "Halal options" },
    { key: "parkingAvailable", label: "Parking available" },
    { key: "wifiAvailable", label: "Free Wi-Fi" },
    { key: "petFriendly", label: "Pet friendly" },
    { key: "wheelchairAccessible", label: "Wheelchair accessible" }
  ],
  HEALTHCARE: [
    { key: "appointmentsRequired", label: "Appointments required" },
    { key: "emergencyCare", label: "Emergency care available" },
    { key: "insuranceAccepted", label: "Insurance accepted" },
    { key: "telemedicineAvailable", label: "Telemedicine available" },
    { key: "pharmacyAvailable", label: "Pharmacy available" },
    { key: "parkingAvailable", label: "Parking available" },
    { key: "wheelchairAccessible", label: "Wheelchair accessible" },
    { key: "labServicesAvailable", label: "Laboratory services" }
  ],
  OTHER: [
    { key: "appointmentsRequired", label: "Appointments required" },
    { key: "parkingAvailable", label: "Parking available" },
    { key: "wifiAvailable", label: "Free Wi-Fi" },
    { key: "wheelchairAccessible", label: "Wheelchair accessible" }
  ]
};

type HourRow = { label: string; closed: boolean; allDay: boolean; open: string; close: string };
const DAYS = [
  ["monday", "Monday"], ["tuesday", "Tuesday"], ["wednesday", "Wednesday"],
  ["thursday", "Thursday"], ["friday", "Friday"], ["saturday", "Saturday"], ["sunday", "Sunday"]
] as const;

function defaultHours(): Record<string, HourRow> {
  return Object.fromEntries(
    DAYS.map(([key, label]) => [key, { label, closed: true, allDay: false, open: "09:00", close: "18:00" }])
  );
}

function parseFactValue(facts: Record<string, string>, label: string): boolean | undefined {
  const value = facts[label];
  if (value == null) return undefined;
  return value.toLowerCase() === "yes" || value.toLowerCase() === "true";
}

function factsFromFacilities(
  facilities: Record<string, boolean>,
  options: { key: string; label: string }[]
): Record<string, string> {
  return Object.fromEntries(options.map(option => [option.label, facilities[option.key] ? "Yes" : "No"]));
}

export default function Onboarding() {
  const nav = useNavigate();
  const id = session.businessId() ?? "";
  const [step, setStep] = useState(0);
  const [catalog, setCatalog] = useState<Catalog | null>(null);
  const [summary, setSummary] = useState<OnboardingSummary | null>(null);
  const [name, setName] = useState("");
  const [type, setType] = useState("");
  const [description, setDescription] = useState("");
  const [services, setServices] = useState<ServiceDefinition[]>([]);
  const [knowledge, setKnowledge] = useState<BusinessKnowledge>(emptyKnowledge);
  const [facilities, setFacilities] = useState<Record<string, boolean>>({});
  const [hours, setHours] = useState<Record<string, HourRow>>(defaultHours);
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [website, setWebsite] = useState("");
  const [locations, setLocations] = useState<LocationEntry[]>([{ id: "location-1", label: "", address: "", lat: null, lng: null }]);
  const [additionalInfo, setAdditionalInfo] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!id) return;
    Promise.all([api.catalog(id), api.summary(id)])
      .then(([c, s]) => { setCatalog(c); setSummary(s); })
      .catch(e => setError(e instanceof Error ? e.message : "Unable to load onboarding data"));
  }, [id]);

  const steps = ["Business", "Services", "Knowledge", "Connect", "Launch"];
  const selected = useMemo(() => new Set(services.map(s => s.requestType)), [services]);
  const facilityOptions = FACILITY_OPTIONS[type] ?? FACILITY_OPTIONS.OTHER;

  useEffect(() => {
    const next: Record<string, boolean> = {};
    facilityOptions.forEach(option => {
      const existing = parseFactValue(knowledge.facts ?? {}, option.label);
      next[option.key] = existing ?? false;
    });
    setFacilities(next);
  // Reinitialize options only when the business type changes.
  // eslint is intentionally not used in this project.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [type]);

  if (!id) {
    return <CreateBusiness onCreated={businessId => {
      session.setBusinessId(businessId);
      window.location.reload();
    }} />;
  }

  async function connectWebsite() {
    setBusy(true);
    setError("");
    try {
      await api.connectWebsite(id);
      const s = await api.summary(id);
      setSummary(s);
      setStep(4);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to connect website channel");
    } finally {
      setBusy(false);
    }
  }

  async function saveAndNext() {
    setBusy(true);
    setError("");
    try {
      if (step === 0) {
        const s = await api.saveBusiness(id, { businessName: name, businessType: type, description });
        setSummary(s);
        setStep(1);
      } else if (step === 1) {
        const s = await api.saveServices(id, services);
        setSummary(s);
        setStep(2);
      } else if (step === 2) {
        const operatingHours = Object.fromEntries(
          DAYS.map(([key]) => {
            const row = hours[key];
            return [row.label, row.closed ? "Closed" : row.allDay ? "24 hours" : `${row.open}-${row.close}`];
          })
        );

        const optionFacts = factsFromFacilities(facilities, facilityOptions);
        const capabilityLabels = facilityOptions.filter(option => facilities[option.key]).map(option => option.label);
        const restrictionLabels = facilityOptions.filter(option => !facilities[option.key]).map(option => `${option.label}: No`);
        const policies = facilityOptions
          .filter(option => option.key === "petsAllowed" || option.key === "smokingAllowed")
          .map(option => facilities[option.key] ? `${option.label}: allowed` : `${option.label}: not allowed`);

        const updatedKnowledge: BusinessKnowledge = {
          ...knowledge,
          businessDescription: description,
          services: services.map(service => service.requestType),
          policies: Array.from(new Set([...(knowledge.policies ?? []), ...policies])),
          instructions: additionalInfo.trim(),
          operatingHours,
          locations: locations.map(item => item.address.trim()).filter(Boolean),
          locationDetails: locations
            .filter(item => item.address.trim() || (item.lat != null && item.lng != null))
            .map(item => ({
              label: item.label.trim(),
              address: item.address.trim(),
              latitude: item.lat,
              longitude: item.lng
            } satisfies BusinessLocation)),
          facts: {
            ...(knowledge.facts ?? {}),
            ...optionFacts,
            ...Object.fromEntries(locations.filter(item => item.lat != null && item.lng != null).flatMap((item, index) => [
              [`Location ${index + 1} latitude`, String(item.lat)],
              [`Location ${index + 1} longitude`, String(item.lng)]
            ]))
          },
          contactInformation: {
            ...(knowledge.contactInformation ?? {}),
            ...(phone.trim() ? { phone: phone.trim() } : {}),
            ...(email.trim() ? { email: email.trim() } : {}),
            ...(website.trim() ? { website: website.trim() } : {})
          },
          capabilities: Array.from(new Set([...(knowledge.capabilities ?? []), ...capabilityLabels])),
          restrictions: Array.from(new Set([...(knowledge.restrictions ?? []), ...restrictionLabels]))
        };

        const s = await api.saveKnowledge(id, updatedKnowledge);
        setKnowledge(updatedKnowledge);
        setSummary(s);
        setStep(3);
      } else if (step === 3) {
        setStep(4);
      } else {
        await api.complete(id);
        nav("/");
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to save");
    } finally {
      setBusy(false);
    }
  }

  function updateHour(key: string, patch: Partial<HourRow>) {
    setHours(previous => ({ ...previous, [key]: { ...previous[key], ...patch } }));
  }

  function copyMonday() {
    const source = hours.monday;
    setHours(previous => Object.fromEntries(DAYS.map(([key]) => [key, { ...previous[key], closed: source.closed, allDay: source.allDay, open: source.open, close: source.close }])));
  }

  function setWeekendClosed() {
    setHours(previous => ({
      ...previous,
      saturday: { ...previous.saturday, closed: true, allDay: false },
      sunday: { ...previous.sunday, closed: true, allDay: false }
    }));
  }

  return <div className="onboarding-page">
    <div className="page-top">
      <div>
        <span className="eyebrow">Setup your receptionist</span>
        <h1>Build your business workspace</h1>
        <p className="muted">A few details now. Your AI uses them as its operating context.</p>
      </div>
      <div className="progress-count">{step + 1}<span>/ {steps.length}</span></div>
    </div>

    <div className="wizard">
      <div className="stepper">
        {steps.map((item, index) => <div className={`step ${index === step ? "active" : ""} ${index < step ? "done" : ""}`} key={item}>
          <span>{index < step ? <Check size={14} /> : index + 1}</span>{item}
        </div>)}
      </div>

      <div className="wizard-card">
        {step === 0 && <>
          <div className="section-heading"><WandSparkles /><div><h2>Tell us about your business</h2><p>We'll tailor your receptionist to your industry.</p></div></div>
          <div className="type-grid">
            {(catalog?.businessTypes ?? []).map(item => {
              const Icon = icons[item.code] ?? WandSparkles;
              return <button type="button" className={`type-card ${type === item.code ? "selected" : ""}`} key={item.code} onClick={() => setType(item.code)}>
                <Icon /><b>{item.name}</b><small>{item.description}</small>{type === item.code && <CheckCircle2 className="selected-check" size={19} />}
              </button>;
            })}
          </div>
          <div className="field-grid">
            <label><span>Business name</span><input value={name} onChange={e => setName(e.target.value)} placeholder="e.g. Your business name" /></label>
            <label><span>Short description</span><input value={description} onChange={e => setDescription(e.target.value)} placeholder="e.g. Family-friendly accommodation and dining" /></label>
          </div>
        </>}

        {step === 1 && <>
          <div className="section-heading"><WandSparkles /><div><h2>Choose what your AI can handle</h2><p>Start with the workflows you want customers to complete.</p></div></div>
          <div className="service-grid">
            {(catalog?.suggestedServices ?? []).map(item => {
              const on = selected.has(item.code);
              return <button type="button" className={`service-card ${on ? "selected" : ""}`} key={item.code} onClick={() => setServices(current => on ? current.filter(x => x.requestType !== item.code) : [...current, item.definition])}>
                <div className="service-icon"><Check size={16} /></div>
                <div><b>{item.name}</b><small>{item.description}</small><span>{item.definition.requiredSlots.length} details collected</span></div>
              </button>;
            })}
          </div>
        </>}

        {step === 2 && <>
          <div className="section-heading"><WandSparkles /><div><h2>Tell Inquiro the important things</h2><p>Use quick options for common policies and facilities, then add anything unique to your business.</p></div></div>

          <section className="knowledge-block">
            <div className="knowledge-block-title"><Sparkles size={17} /><div><h3>{type === "HOSPITALITY" ? "Facilities & policies" : type === "RESTAURANT" ? "Facilities & dining options" : type === "HEALTHCARE" ? "Services & accessibility" : "Business options"}</h3><p>Tap an item to mark it as available. Unselected items are saved as not available.</p></div></div>
            <div className="option-grid">
              {facilityOptions.map(option => <label className={`option-card ${facilities[option.key] ? "selected" : ""}`} key={option.key}>
                <input type="checkbox" checked={Boolean(facilities[option.key])} onChange={e => setFacilities(current => ({ ...current, [option.key]: e.target.checked }))} />
                <span className="custom-check">{facilities[option.key] && <Check size={14} />}</span>
                <span><b>{option.label}</b>{option.hint && <small>{option.hint}</small>}</span>
              </label>)}
            </div>
          </section>

          <section className="knowledge-block">
            <div className="knowledge-block-title"><Clock3 size={17} /><div><h3>Opening hours</h3><p>Set each day independently, mark closed days, or use 24-hour mode.</p></div></div>
            <div className="hours-actions">
              <button type="button" className="text-button" onClick={copyMonday}>Copy Monday to all days</button>
              <button type="button" className="text-button" onClick={setWeekendClosed}>Set weekend closed</button>
            </div>
            <div className="hours-list">
              {DAYS.map(([key]) => {
                const row = hours[key];
                return <div className="hours-row" key={key}>
                  <b>{row.label}</b>
                  <label className="mini-toggle"><input type="checkbox" checked={row.closed} onChange={e => updateHour(key, { closed: e.target.checked, allDay: e.target.checked ? false : row.allDay })} /><span className="mini-check">{row.closed && <Check size={11} />}</span>Closed</label>
                  <label className="mini-toggle"><input type="checkbox" disabled={row.closed} checked={row.allDay} onChange={e => updateHour(key, { allDay: e.target.checked })} /><span className="mini-check">{row.allDay && <Check size={11} />}</span>24 hours</label>
                  <input className="time-input" type="time" value={row.open} disabled={row.closed || row.allDay} onChange={e => updateHour(key, { open: e.target.value })} />
                  <span className="time-separator">to</span>
                  <input className="time-input" type="time" value={row.close} disabled={row.closed || row.allDay} onChange={e => updateHour(key, { close: e.target.value })} />
                </div>;
              })}
            </div>
          </section>

          <section className="knowledge-block">
            <div className="knowledge-block-title"><MessageCircle size={17} /><div><h3>Contact & location</h3><p>Help customers find and contact the business without typing repetitive details.</p></div></div>
            <div className="knowledge-grid">
              <div className="location-manager"><div className="location-entry-list">{locations.map((item,index)=><div className="location-entry" key={item.id}><div className="location-entry-head"><strong>{item.label.trim() || `Location ${index+1}`}</strong>{locations.length>1&&<button type="button" className="remove-location" onClick={()=>setLocations(current=>current.filter(x=>x.id!==item.id))}>Remove</button>}</div><input value={item.label} onChange={e=>setLocations(current=>current.map(x=>x.id===item.id?{...x,label:e.target.value}:x))} placeholder="Location name (e.g. Main branch)" /><input value={item.address} onChange={e=>setLocations(current=>current.map(x=>x.id===item.id?{...x,address:e.target.value}:x))} placeholder="e.g. 123 Galle Road, Matara" /><MapPicker value={item} onChange={next=>setLocations(current=>current.map(x=>x.id===item.id?next:x))} /></div>)}</div><button type="button" className="add-location-button" onClick={()=>setLocations(current=>[...current,{id:`location-${Date.now()}`,label:"",address:"",lat:null,lng:null}])}>+ Add another location</button></div>
              <label><span>Phone</span><input value={phone} onChange={e => setPhone(e.target.value)} placeholder="e.g. +94 71 234 5678" /></label>
              <label><span>Email</span><input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="e.g. hello@yourbusiness.com" /></label>
              <label><span>Website</span><input value={website} onChange={e => setWebsite(e.target.value)} placeholder="e.g. https://yourbusiness.com" /></label>
            </div>
          </section>

          <section className="knowledge-block">
            <div className="knowledge-block-title"><WandSparkles size={17} /><div><h3>Anything else?</h3><p>Add special instructions, house rules, exceptions, or information that doesn't fit the options above.</p></div></div>
            <textarea className="wide-textarea" rows={5} value={additionalInfo} onChange={e => setAdditionalInfo(e.target.value)} placeholder="e.g. Check-in starts at 2 PM. Quiet hours are 10 PM–7 AM. Children under 6 stay free. Ask a team member about special requests." />
          </section>
        </>}

        {step === 3 && <ChannelStep summary={summary} busy={busy} onConnect={connectWebsite} />}
        {step === 4 && <LaunchStep summary={summary} />}

        {error && <div className="error-box">{error}</div>}

        <div className="wizard-actions">
          {step > 0 ? <button type="button" className="secondary-button" onClick={() => setStep(step - 1)}><ArrowLeft size={17} />Back</button> : <span />}
          {step === 3 ? <button type="button" className="secondary-button" onClick={() => setStep(4)} disabled={busy}>Skip for now<ArrowRight size={17} /></button> : <button type="button" className="primary-button" onClick={step === 3 ? connectWebsite : saveAndNext} disabled={busy || (step === 0 && (!name || !type)) || (step === 1 && services.length === 0)}>{busy ? "Saving…" : step === 4 ? "Go to dashboard" : step === 3 ? "Enable website chat" : "Continue"}<ArrowRight size={17} /></button>}
        </div>
      </div>
    </div>
  </div>;
}

function CreateBusiness({ onCreated }: { onCreated: (id: string) => void }) {
  const [name, setName] = useState("");
  const [type, setType] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function create() {
    setBusy(true);
    setError("");
    try {
      const r = await api.createBusiness(name, type, "");
      onCreated(r.businessId);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to create business");
    } finally {
      setBusy(false);
    }
  }
  return <div className="empty-state">
    <div className="empty-icon"><WandSparkles /></div>
    <h1>Create your business</h1>
    <p>Let's create the workspace your AI receptionist will operate in.</p>
    <div className="field-stack">
      <label><span>Business name</span><input value={name} onChange={e => setName(e.target.value)} placeholder="e.g. Your business name" /></label>
      <label><span>Business type</span><select value={type} onChange={e => setType(e.target.value)}><option value="">Choose…</option><option value="HOSPITALITY">Hotel / Accommodation</option><option value="RESTAURANT">Restaurant</option><option value="HEALTHCARE">Clinic / Healthcare</option></select></label>
      {error && <div className="error-box">{error}</div>}
      <button type="button" className="primary-button" disabled={!name || !type || busy} onClick={create}>{busy ? "Creating…" : "Create workspace"}<ArrowRight size={17} /></button>
    </div>
  </div>;
}

function ChannelStep({ summary, busy, onConnect }: { summary: OnboardingSummary | null; busy: boolean; onConnect: () => void }) {
  return <div className="connect-panel">
    <div className="channel-logo"><MessageCircle /></div>
    <h2>Connect a customer channel</h2>
    <p>Website chat is the fastest way to test your receptionist locally. Messenger and WhatsApp can be connected later from your business settings.</p>
    <div className="channel-status">
      <span className={summary?.channelConfigured ? "ok-dot" : "pending-dot"} />
      <div><b>{summary?.channelConfigured ? "Channel connected" : "No channel connected yet"}</b><small>{summary?.channelConfigured ? "Your receptionist can receive customer messages." : "Connect website chat to continue with the local demo."}</small></div>
      {!summary?.channelConfigured && <button type="button" className="primary-button channel-connect" disabled={busy} onClick={onConnect}>{busy ? "Connecting…" : "Enable website chat"}<ArrowRight size={16} /></button>}
    </div>
  </div>;
}

function LaunchStep({ summary }: { summary: OnboardingSummary | null }) {
  return <div className="launch-panel">
    <div className="launch-icon"><PartyPopper /></div>
    <h2>Your receptionist is configured</h2>
    <p>Review the essentials below. You can keep refining the workspace after launch.</p>
    <div className="check-list">{[
      ["Business information", summary?.businessInformationComplete],
      ["Services", summary?.servicesConfigured],
      ["Knowledge", summary?.knowledgeConfigured],
      ["Channel", summary?.channelConfigured]
    ].map(([label, ok]) => <div key={String(label)}><span className={ok ? "check-circle" : "pending-circle"}>{ok && <Check size={13} />}</span><span>{String(label)}</span><b>{ok ? "Ready" : "Next"}</b></div>)}</div>
  </div>;
}


type LocationEntry = { id: string; label: string; address: string; lat: number | null; lng: number | null };

const sriLankaCenter: [number, number] = [7.8731, 80.7718];

function LocationMarker({ value, onChange }: { value: LocationEntry; onChange: (next: LocationEntry) => void }) {
  useMapEvents({
    click(event) {
      onChange({ ...value, lat: event.latlng.lat, lng: event.latlng.lng });
    }
  });
  return value.lat != null && value.lng != null ? (
    <Marker position={[value.lat, value.lng]}><Popup>Business location</Popup></Marker>
  ) : null;
}

function MapPicker({ value, onChange }: { value: LocationEntry; onChange: (next: LocationEntry) => void }) {
  const [query, setQuery] = useState("");
  const [searching, setSearching] = useState(false);
  const [mapError, setMapError] = useState("");

  async function searchAddress() {
    if (!query.trim()) return;
    setSearching(true); setMapError("");
    try {
      const response = await fetch(`https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=${encodeURIComponent(query.trim())}`, {
        headers: { Accept: "application/json" }
      });
      if (!response.ok) throw new Error("Address search failed");
      const results = await response.json() as { display_name: string; lat: string; lon: string }[];
      if (!results.length) { setMapError("Address not found. Try a more specific address."); return; }
      const result = results[0];
      onChange({ ...value, address: result.display_name, lat: Number(result.lat), lng: Number(result.lon) });
    } catch (error) {
      setMapError(error instanceof Error ? error.message : "Unable to search address");
    } finally { setSearching(false); }
  }

  function useCurrentLocation() {
    setMapError("");
    if (!navigator.geolocation) { setMapError("Location is not available in this browser."); return; }
    navigator.geolocation.getCurrentPosition(
      position => onChange({ ...value, lat: position.coords.latitude, lng: position.coords.longitude }),
      () => setMapError("Location permission was not granted. You can still search or place the pin manually."),
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  const center: [number, number] = value.lat != null && value.lng != null ? [value.lat, value.lng] : sriLankaCenter;
  return <div className="map-picker">
    <div className="map-tools">
      <div className="map-search"><input value={query} onChange={e=>setQuery(e.target.value)} onKeyDown={e=>{if(e.key==="Enter"){e.preventDefault();void searchAddress()}}} placeholder="Search this location on the map" /><button type="button" className="secondary-button" onClick={()=>void searchAddress()} disabled={searching}>{searching ? "Searching…" : "Search"}</button></div>
      <button type="button" className="text-button" onClick={useCurrentLocation}>Use my current location</button>
    </div>
    <MapContainer center={center} zoom={value.lat != null ? 15 : 7} scrollWheelZoom className="business-map">
      <TileLayer attribution='&copy; OpenStreetMap contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
      <LocationMarker value={value} onChange={onChange} />
    </MapContainer>
    <div className="map-hint">Search an address, use your current location, or click the map to place the pin. You can still edit the address manually above.</div>
    {value.lat != null && value.lng != null && <div className="coordinates">📍 {value.lat.toFixed(6)}, {value.lng.toFixed(6)}</div>}
    {mapError && <div className="map-error">{mapError}</div>}
  </div>;
}
