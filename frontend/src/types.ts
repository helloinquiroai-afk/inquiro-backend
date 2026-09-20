export type BusinessType = "HOSPITALITY" | "RESTAURANT" | "HEALTHCARE" | "OTHER";
export type ServiceDefinition = {
  requestType: string; description: string; requiredSlots: string[];
  slotPrompts: Record<string,string>; actionType: string;
  actionRequiredSlots: string[]; availabilityStrategy: string;
  availabilityFields: Record<string,string>;
};
export type BusinessKnowledge = {
  businessDescription: string; services: string[]; products: string[];
  facts: Record<string,string>; faqs: string[]; policies: string[];
  instructions: string; operatingHours: Record<string,string>; locations: string[];
  contactInformation: Record<string,string>; bookingRules: Record<string,string>;
  capabilities: string[]; restrictions: string[];
};
export type ConversationResponse = {
  inquiry: { domain?: string; service?: string; fields?: Record<string, unknown> } | null;
  missingFields: string[];
  status: string;
  reply: string;
  bookingId?: string;
};
export type OnboardingSummary = {
  businessId: string; status: string; businessInformationComplete: boolean;
  servicesConfigured: boolean; knowledgeConfigured: boolean; channelConfigured: boolean;
  readyForReceptionist: boolean; missingRequirements: string[];
};
export type Catalog = {
  businessTypes: { code:string; name:string; description:string }[];
  suggestedServices: { code:string; name:string; description:string; definition:ServiceDefinition }[];
};
export type Booking = {
  bookingId:string; businessId:string; service:string; bookingDate:string|null;
  checkOutDate:string|null; durationNights:number|null; startTime:string|null;
  endTime:string|null; customerName:string; customerPhone:string;
  status:string; createdAt:string;
};