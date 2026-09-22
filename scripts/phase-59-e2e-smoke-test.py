#!/usr/bin/env python3
import json
import os
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("BASE_URL", "http://127.0.0.1:8080").rstrip("/")

def request(method, path, body=None, token=None, expected=None, headers=None):
    data = None if body is None else json.dumps(body).encode()
    req_headers = {"Content-Type": "application/json"}
    if token:
        req_headers["Authorization"] = f"Bearer {token}"
    if headers:
        req_headers.update(headers)
    req = urllib.request.Request(BASE + path, data=data, headers=req_headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            status = response.status
            raw = response.read().decode()
            parsed = json.loads(raw) if raw else None
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode()
        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = raw
        raise AssertionError(f"{method} {path}: expected {expected}, got {exc.code}: {parsed}") from exc
    if expected is not None and status != expected:
        raise AssertionError(f"{method} {path}: expected {expected}, got {status}: {parsed}")
    return parsed

def assert_true(condition, message):
    if not condition:
        raise AssertionError(message)

print("1. Health/readiness")
health = request("GET", "/actuator/health/readiness", expected=200)
assert_true(health.get("status") == "UP", f"Readiness is not UP: {health}")

suffix = "phase59@example.test"
password = "Phase59-Secure-Password-123!"
print("2. Register business user")
user = request("POST", "/api/auth/register", {
    "email": suffix,
    "name": "Phase 59 E2E",
    "password": password
}, expected=201)
assert_true(user.get("email") == suffix, "Registration did not return the created user")

print("3. Login and obtain bearer session")
login = request("POST", "/api/auth/login", {
    "email": suffix,
    "password": password
}, expected=200)
token = login.get("accessToken")
assert_true(token, "Login did not return an access token")

print("4. Create tenant business")
service = {
    "requestType": "ROOM_BOOKING",
    "description": "Phase 59 room booking",
    "requiredSlots": ["checkInDate", "durationNights", "guestCount"],
    "slotPrompts": {},
    "actionType": "BOOKING",
    "actionRequiredSlots": ["customerName", "customerPhone"],
    "availabilityStrategy": "DATE_RANGE",
    "availabilityFields": {}
}
business = request("POST", "/api/business/accounts", {
    "businessName": "Phase 59 Demo Hotel",
    "businessType": "HOSPITALITY",
    "description": "Disposable E2E test business",
    "services": [service],
    "knowledge": {
        "businessDescription": "Disposable E2E hotel",
        "services": ["Room booking"],
        "products": [],
        "facts": {},
        "faqs": [],
        "policies": [],
        "instructions": "",
        "operatingHours": {
            "MONDAY": "00:00-23:59",
            "TUESDAY": "00:00-23:59",
            "WEDNESDAY": "00:00-23:59",
            "THURSDAY": "00:00-23:59",
            "FRIDAY": "00:00-23:59",
            "SATURDAY": "00:00-23:59",
            "SUNDAY": "00:00-23:59"
        },
        "locations": ["Paris"],
        "contactInformation": {},
        "bookingRules": {},
        "capabilities": ["Room booking"],
        "restrictions": [],
        "boundaries": {
            "supported": ["ROOM_BOOKING"],
            "notSupported": [],
            "requiresHuman": []
        }
    }
}, token=token, expected=201)
business_id = business.get("businessId")
assert_true(business_id, "Business creation did not return businessId")

print("5. Add website channel and configure origin")
channel = request("POST", f"/api/business/accounts/{business_id}/channels", {
    "type": "WEBSITE",
    "externalId": "phase59-web",
    "enabled": True
}, token=token, expected=201)
channel_id = channel.get("channelId")
assert_true(channel_id, "Channel creation did not return channelId")

configured = request("PUT", f"/api/business/accounts/{business_id}/channels/{channel_id}/website", {
    "allowedOrigins": ["https://phase59.example.test"]
}, token=token, expected=200)
assert_true("https://phase59.example.test" in configured.get("allowedOrigins", []), "Website origin was not persisted")

print("6. Configure booking inventory")
inventory = request("PUT", f"/api/business/accounts/{business_id}/booking-inventory", {
    "service": "ROOM_BOOKING",
    "capacity": 1
}, token=token, expected=200)
assert_true(inventory.get("capacity") == 1, "Inventory capacity was not persisted")

fields = {
    "checkInDate": "2099-06-10",
    "durationNights": 2,
    "guestCount": 2
}

print("7. Check availability")
available = request("POST", f"/api/business/accounts/{business_id}/availability", {
    "service": "ROOM_BOOKING",
    "fields": fields
}, token=token, expected=200)
assert_true(available.get("status") == "CONFIRMED", f"Expected confirmed availability: {available}")

print("8. Create booking with idempotency key")
booking = request("POST", f"/api/business/accounts/{business_id}/bookings", {
    "service": "ROOM_BOOKING",
    "fields": fields,
    "customerName": "Phase 59 Customer",
    "customerPhone": "+94770000000"
}, token=token, headers={"Idempotency-Key": "phase59-booking-001"}, expected=201)
booking_id = booking.get("bookingId")
assert_true(booking_id, "Booking creation did not return bookingId")
assert_true(booking.get("status") == "CONFIRMED", f"Booking was not confirmed: {booking}")

print("9. Verify idempotent retry returns the same booking")
retry = request("POST", f"/api/business/accounts/{business_id}/bookings", {
    "service": "ROOM_BOOKING",
    "fields": fields,
    "customerName": "Phase 59 Customer",
    "customerPhone": "+94770000000"
}, token=token, headers={"Idempotency-Key": "phase59-booking-001"}, expected=201)
assert_true(retry.get("bookingId") == booking_id, "Idempotency retry created a different booking")

print("10. Verify capacity is consumed")
full = request("POST", f"/api/business/accounts/{business_id}/availability", {
    "service": "ROOM_BOOKING",
    "fields": fields
}, token=token, expected=200)
assert_true(full.get("status") == "UNAVAILABLE", f"Expected unavailable capacity: {full}")

print("11. Cancel booking")
cancelled = request("POST", f"/api/business/accounts/{business_id}/bookings/{booking_id}/cancel", token=token, expected=200)
assert_true(cancelled.get("status") == "CANCELLED", f"Booking was not cancelled: {cancelled}")

print("12. Verify capacity is released")
available_again = request("POST", f"/api/business/accounts/{business_id}/availability", {
    "service": "ROOM_BOOKING",
    "fields": fields
}, token=token, expected=200)
assert_true(available_again.get("status") == "CONFIRMED", f"Expected inventory to be released: {available_again}")

print("13. Verify tenant protection")
other_email = "phase59-other@example.test"
request("POST", "/api/auth/register", {
    "email": other_email,
    "name": "Other Tenant",
    "password": password
}, expected=201)
other_login = request("POST", "/api/auth/login", {
    "email": other_email,
    "password": password
}, expected=200)
other_token = other_login.get("accessToken")
try:
    request("GET", f"/api/business/accounts/{business_id}/onboarding", token=other_token, expected=403)
except AssertionError as exc:
    if "got 404" in str(exc):
        raise AssertionError("Tenant authorization must return 403 for an existing business")
    raise

print("14. Logout")
request("POST", "/api/auth/logout", token=token, expected=204)

print("Phase 59 E2E smoke test passed")
