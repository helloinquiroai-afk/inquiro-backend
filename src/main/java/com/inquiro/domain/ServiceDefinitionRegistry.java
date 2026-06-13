package com.inquiro.domain;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ServiceDefinitionRegistry {

    private final Map<String, ServiceDefinition> definitions =
            Map.of(

                    "ROOM_BOOKING",
                    new ServiceDefinition(
                            "ROOM_BOOKING",
                            List.of(
                                    "location",
                                    "checkInDate",
                                    "guestCount",
                                    "durationNights"
                            )
                    ),

                    "AIRPORT_PICKUP",
                    new ServiceDefinition(
                            "AIRPORT_PICKUP",
                            List.of(
                                    "pickupLocation",
                                    "date",
                                    "time",
                                    "passengerCount"
                            )
                    ),

                    "TABLE_RESERVATION",
                    new ServiceDefinition(
                            "TABLE_RESERVATION",
                            List.of(
                                    "date",
                                    "time",
                                    "guestCount"
                            )
                    ),

                    "BUFFET_RESERVATION",
                    new ServiceDefinition(
                            "BUFFET_RESERVATION",
                            List.of(
                                    "date",
                                    "guestCount"
                            )
                    ),

                    "DOCTOR_APPOINTMENT",
                    new ServiceDefinition(
                            "DOCTOR_APPOINTMENT",
                            List.of(
                                    "specialty",
                                    "date"
                            )
                    )
            );

    public ServiceDefinition get(String serviceCode) {
        return definitions.get(serviceCode);
    }
}