package com.inquiro.request;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RequestRegistry {

    private final Map<String, RequestDefinition>
            definitions =
            Map.of(

                    "BOOK_ACCOMMODATION",
                    new RequestDefinition(
                            "BOOK_ACCOMMODATION",
                            List.of(
                                    "location",
                                    "checkInDate",
                                    "guestCount"
                            )
                    ),

                    "BOOK_RESTAURANT_TABLE",
                    new RequestDefinition(
                            "BOOK_RESTAURANT_TABLE",
                            List.of(
                                    "date",
                                    "time",
                                    "guestCount"
                            )
                    ),

                    "BOOK_DOCTOR_APPOINTMENT",
                    new RequestDefinition(
                            "BOOK_DOCTOR_APPOINTMENT",
                            List.of(
                                    "specialty",
                                    "date"
                            )
                    ),

                    "ARRANGE_AIRPORT_PICKUP",
                    new RequestDefinition(
                            "ARRANGE_AIRPORT_PICKUP",
                            List.of(
                                    "pickupLocation",
                                    "date",
                                    "passengerCount"
                            )
                    )
            );

    public RequestDefinition get(
            String requestType) {

        return definitions.get(requestType);
    }
}
