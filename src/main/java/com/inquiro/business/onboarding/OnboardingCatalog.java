package com.inquiro.business.onboarding;

import java.util.List;
import java.util.Map;

import com.inquiro.request.RequestActionType;
import com.inquiro.request.RequestDefinition;

public final class OnboardingCatalog {

    private OnboardingCatalog() {
    }

    public static List<BusinessTypeOption> businessTypes() {
        return List.of(
                new BusinessTypeOption("HOSPITALITY", "Hotel / Accommodation",
                        "Hotels, guesthouses, villas and other accommodation businesses."),
                new BusinessTypeOption("RESTAURANT", "Restaurant",
                        "Restaurants, cafes, buffet services and dining businesses."),
                new BusinessTypeOption("HEALTHCARE", "Clinic / Healthcare",
                        "Clinics, medical centers and healthcare providers."),
                new BusinessTypeOption("OTHER", "Other business",
                        "Configure a custom business service.")
        );
    }

    public static List<ServiceTemplate> servicesFor(String businessType) {
        String type = businessType == null ? "" : businessType.trim().toUpperCase();

        return switch (type) {
            case "HOSPITALITY" -> List.of(
                    service("ROOM_BOOKING", "Room booking",
                            "Help customers ask about and book rooms.",
                            List.of("arrival", "departure", "guestCount"),
                            Map.of(
                                    "arrival", "What is your check-in date?",
                                    "departure", "What is your check-out date?",
                                    "guestCount", "How many guests will be staying?"
                            ),
                            List.of("customerName", "customerPhone"),
                            "DATE_RANGE",
                            Map.of("startDate", "arrival", "endDate", "departure")),
                    service("AIRPORT_PICKUP", "Airport pickup",
                            "Collect airport transfer requests from customers.",
                            List.of("airport", "arrivalDate", "arrivalTime", "guestCount"),
                            Map.of(
                                    "airport", "Which airport will you arrive at?",
                                    "arrivalDate", "What is your arrival date?",
                                    "arrivalTime", "What is your arrival time?",
                                    "guestCount", "How many guests need pickup?"
                            ),
                            List.of("customerName", "customerPhone"),
                            "TIME_SLOT",
                            Map.of("date", "arrivalDate", "startTime", "arrivalTime"))
            );
            case "RESTAURANT" -> List.of(
                    service("TABLE_RESERVATION", "Table reservation",
                            "Help customers reserve a table.",
                            List.of("date", "time", "guestCount"),
                            Map.of(
                                    "date", "What date would you like?",
                                    "time", "What time would you like?",
                                    "guestCount", "How many people will be dining?"
                            ),
                            List.of("customerName", "customerPhone"),
                            "TIME_SLOT",
                            Map.of("date", "date", "startTime", "time")),
                    service("BUFFET_RESERVATION", "Buffet reservation",
                            "Help customers reserve buffet dining.",
                            List.of("date", "time", "guestCount"),
                            Map.of(
                                    "date", "What date would you like?",
                                    "time", "What time would you like?",
                                    "guestCount", "How many people?"
                            ),
                            List.of("customerName", "customerPhone"),
                            "TIME_SLOT",
                            Map.of("date", "date", "startTime", "time"))
            );
            case "HEALTHCARE" -> List.of(
                    service("DOCTOR_APPOINTMENT", "Doctor appointment",
                            "Help patients request a doctor appointment.",
                            List.of("specialty", "appointmentDate", "appointmentTime"),
                            Map.of(
                                    "specialty", "Which medical specialty do you need?",
                                    "appointmentDate", "What date would you prefer?",
                                    "appointmentTime", "What time would you prefer?"
                            ),
                            List.of("customerName", "customerPhone"),
                            "TIME_SLOT",
                            Map.of(
                                    "date", "appointmentDate",
                                    "startTime", "appointmentTime",
                                    "endTime", "appointmentEndTime"))
            );
            default -> List.of();
        };
    }

    private static ServiceTemplate service(
            String code,
            String name,
            String description,
            List<String> requiredSlots,
            Map<String, String> slotPrompts,
            List<String> actionRequiredSlots,
            String availabilityStrategy,
            Map<String, String> availabilityFields) {

        return new ServiceTemplate(
                code,
                name,
                description,
                new RequestDefinition(
                        code,
                        description,
                        requiredSlots,
                        slotPrompts,
                        RequestActionType.BOOKING,
                        actionRequiredSlots,
                        availabilityStrategy,
                        availabilityFields
                )
        );
    }

    public record BusinessTypeOption(
            String code,
            String name,
            String description) {
    }

    public record ServiceTemplate(
            String code,
            String name,
            String description,
            RequestDefinition definition) {
    }
}
