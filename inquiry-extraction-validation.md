# Inquiry Extraction Validation

Date: 2026-06-10

## Raw Responses

| # | Message | AI Response |
|---|---|---|
| 1 | Need a room in Colombo next Friday for 2 adults | `{"domain":"HOSPITALITY","service":"ROOM_BOOKING","fields":{"location":"Colombo","guestCount":2,"checkInDate":"next Friday"}}` |
| 2 | Need airport pickup from BIA tomorrow at 10 PM for 4 passengers | `{"domain":"HOSPITALITY","service":"AIRPORT_PICKUP","fields":{"pickupLocation":"BIA","date":"tomorrow","time":"10 PM","passengerCount":4}}` |
| 3 | Book a table for 6 people tomorrow at 7 PM | `{"domain":"HOSPITALITY","service":"TABLE_RESERVATION","fields":{"guestCount":6,"date":"tomorrow","time":"7 PM"}}` |
| 4 | Need buffet tomorrow for 8 people | `{"domain":"HOSPITALITY","service":"BUFFET_RESERVATION","fields":{"date":"tomorrow","guestCount":8}}` |
| 5 | Need appointment with a cardiologist tomorrow morning | `{"domain":"HEALTHCARE","service":"DOCTOR_APPOINTMENT","fields":{"specialty":"CARDIOLOGY","date":"tomorrow","timePeriod":"morning"}}` |
| 6 | Need a hotel room | `{"domain":"HOSPITALITY","service":"ROOM_BOOKING","fields":{}}` |
| 7 | Need airport pickup | `{"domain":"HOSPITALITY","service":"AIRPORT_PICKUP","fields":{}}` |
| 8 | Need a room in Colombo and airport pickup from BIA tomorrow | `{"domain":"HOSPITALITY","service":"ROOM_BOOKING","fields":{"location":"Colombo"}}` |
| 9 | Need a booking tomorrow | `{"domain":"HOSPITALITY","service":"BOOKING","fields":{"date":"tomorrow"}}` |
| 10 | I want to reserve dinner for my family tonight | `{"domain":"HOSPITALITY","service":"DINNER_RESERVATION","fields":{"date":"tonight","guestCount":"family"}}` |
| 11 | Can I see a dentist this weekend? | `{"domain":"HEALTHCARE","service":"APPOINTMENT_BOOKING","fields":{"specialty":"DENTIST","date":"this weekend"}}` |
| 12 | Need a room near Galle Face for 3 nights for one person | `{"domain":"HOSPITALITY","service":"ROOM_BOOKING","fields":{"location":"Galle Face","stayDuration":"3 nights","guestCount":1}}` |

## First-Pass Accuracy

| # | Message Type | Domain Correct? | Service Correct? | Fields Correct? | Notes |
|---|---|---|---|---|---|
| 1 | Room booking | Yes | Yes | Yes | Good extraction. |
| 2 | Airport pickup | Yes | Yes | Yes | Good extraction. |
| 3 | Table reservation | Needs decision | Yes | Yes | If restaurant requests must use `RESTAURANT`, domain is wrong. If grouped under hospitality, it is fine. |
| 4 | Buffet reservation | Needs decision | Yes | Yes | Same domain decision as table reservation. |
| 5 | Doctor appointment | Yes | Yes | Yes | Good extraction. |
| 6 | Incomplete room booking | Yes | Yes | Yes | Correctly avoided inventing missing fields. |
| 7 | Incomplete airport pickup | Yes | Yes | Yes | Correctly avoided inventing missing fields. |
| 8 | Multi-service request | Yes | Partial | Partial | Captured room booking but dropped airport pickup. |
| 9 | Ambiguous booking | Partial | No | Yes | Should probably return `UNKNOWN` instead of generic `BOOKING`. |
| 10 | Dinner reservation | Needs decision | Partial | Partial | `guestCount` as `family` is weak; should be omitted or treated as note. |
| 11 | Dentist appointment | Yes | Partial | Yes | Service should probably be `DOCTOR_APPOINTMENT` or `DENTAL_APPOINTMENT`, not generic `APPOINTMENT_BOOKING`. |
| 12 | Room booking | Yes | Yes | Yes | Good extraction. |

## Summary

- Raw JSON format: 12/12 valid-looking JSON responses.
- Markdown fences or explanations: 0/12.
- Clear domain/service extraction is good for simple single-service requests.
- Main failure cases are ambiguous requests, multi-service requests, and inconsistent service naming.

## Before Jackson Parsing

Decisions:

1. Restaurant/table/buffet requests should use domain `RESTAURANT`.
2. Ambiguous requests should return domain `UNKNOWN` and service `UNKNOWN`.
3. Multi-service requests need a future `InquiryAnalysis` model containing a list of `InquiryResult`.
4. For the current single-result contract, multi-service requests return the strongest primary service.
5. Service names should be normalized to this allowed list: `ROOM_BOOKING`, `AIRPORT_PICKUP`, `TABLE_RESERVATION`, `BUFFET_RESERVATION`, `DOCTOR_APPOINTMENT`, `UNKNOWN`.

## First Iteration Score

| Area | Score |
|---|---|
| JSON validity | 10/10 |
| Domain detection | 8/10 |
| Service detection | 8/10 |
| Field extraction | 9/10 |
| Ambiguous handling | 5/10 |
| Multi-service handling | 3/10 |

Overall: 8/10.

## Second Run After Prompt Contract Fix

Date: 2026-06-10

### Raw Responses

| # | Message | AI Response |
|---|---|---|
| 1 | Need a room in Colombo next Friday for 2 adults | `{"domain":"HOSPITALITY","service":"ROOM_BOOKING","fields":{"location":"Colombo","guestCount":2,"checkInDate":"next Friday"}}` |
| 2 | Need airport pickup from BIA tomorrow at 10 PM for 4 passengers | `{"domain":"HOSPITALITY","service":"AIRPORT_PICKUP","fields":{"pickupLocation":"BIA","date":"tomorrow","time":"10 PM","passengerCount":4}}` |
| 3 | Book a table for 6 people tomorrow at 7 PM | `{"domain":"RESTAURANT","service":"TABLE_RESERVATION","fields":{"guestCount":6,"date":"tomorrow","time":"7 PM"}}` |
| 4 | Need buffet tomorrow for 8 people | `{"domain":"RESTAURANT","service":"BUFFET_RESERVATION","fields":{"date":"tomorrow","guestCount":8}}` |
| 5 | Need appointment with a cardiologist tomorrow morning | `{"domain":"HEALTHCARE","service":"DOCTOR_APPOINTMENT","fields":{"specialty":"CARDIOLOGY","date":"tomorrow","timePeriod":"morning"}}` |
| 6 | Need a hotel room | `{"domain":"HOSPITALITY","service":"ROOM_BOOKING","fields":{}}` |
| 7 | Need airport pickup | `{"domain":"HOSPITALITY","service":"AIRPORT_PICKUP","fields":{}}` |
| 8 | Need a room in Colombo and airport pickup from BIA tomorrow | `{"domain":"HOSPITALITY","service":"ROOM_BOOKING","fields":{"location":"Colombo","date":"tomorrow"}}` |
| 9 | Need a booking tomorrow | `{"domain":"UNKNOWN","service":"UNKNOWN","fields":{"date":"tomorrow"}}` |
| 10 | I want to reserve dinner for my family tonight | `{"domain":"RESTAURANT","service":"TABLE_RESERVATION","fields":{"date":"tonight","meal":"dinner"}}` |
| 11 | Can I see a dentist this weekend? | `{"domain":"HEALTHCARE","service":"DOCTOR_APPOINTMENT","fields":{"specialty":"DENTISTRY","date":"this weekend"}}` |
| 12 | Need a room near Galle Face for 3 nights for one person | `{"domain":"HOSPITALITY","service":"ROOM_BOOKING","fields":{"location":"Galle Face","guestCount":1,"durationNights":3}}` |

### Targeted Improvements

| # | Target | Result | Status |
|---|---|---|---|
| 3 | `RESTAURANT` + `TABLE_RESERVATION` | Correct | Pass |
| 4 | `RESTAURANT` + `BUFFET_RESERVATION` | Correct | Pass |
| 9 | `UNKNOWN` + `UNKNOWN` | Correct | Pass |
| 10 | Avoid invented `guestCount:"family"` | Correct; returned `meal:"dinner"` and omitted guest count | Pass |
| 11 | Normalize dentist to `DOCTOR_APPOINTMENT` | Correct | Pass |

### Remaining Issue

Multi-service request #8 still confirms the known architecture limitation: the current single `InquiryResult` contract returns one primary service only. It also attached `date:"tomorrow"` to the room booking even though the date likely belongs to airport pickup. This should be handled later by an `InquiryAnalysis` model containing multiple `InquiryResult` items.

### Second Run Conclusion

- JSON validity: 12/12.
- Fixed-domain behavior improved.
- Fixed-service behavior improved.
- Ambiguous request handling improved.
- Field anti-invention improved.
- Single-service requests are stable enough for Jackson parsing into `InquiryResult`.
