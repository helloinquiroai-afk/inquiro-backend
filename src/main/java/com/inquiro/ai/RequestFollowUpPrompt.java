package com.inquiro.ai;

import java.util.List;
import java.util.Map;

public final class RequestFollowUpPrompt {

    private RequestFollowUpPrompt() {
    }

    public static String build(
            String requestType,
            Map<String, Object> currentFields,
            List<String> missingFields,
            String customerReply) {

        return """
                You are a customer follow-up understanding engine.

                Current Request Type:
                %s

                Known Information:
                %s

                Missing Information:
                %s

                Customer Reply:
                %s

                Extract ONLY information related to the missing fields.

                Return ONLY valid JSON.

                Format:

                {
                  "entities": {}
                }

                Example:

                Current Request Type:
                BOOK_ACCOMMODATION

                Known Information:
                {
                  "location":"Paris"
                }

                Missing Information:
                durationNights

                Customer Reply:
                3 nights

                Output:
                {
                  "entities":{
                    "durationNights":3
                  }
                }

                Return JSON only.
                Example:
                
                Current Request Type:
                BOOK_ACCOMMODATION
                
                Known Information:
                {
                  "location":"Paris",
                  "checkInDate":"next Friday"
                }
                
                Missing Information:
                guestCount
                durationNights
                
                Customer Reply:
                2 adults for 3 nights
                
                Output:
                {
                  "entities":{
                    "guestCount":2,
                    "durationNights":3
                  }
                }
                Example:
                
                Current Request Type:
                BOOK_DOCTOR_APPOINTMENT
                
                Known Information:
                {
                  "specialty":"cardiologist"
                }
                
                Missing Information:
                date
                
                Customer Reply:
                next Monday
                
                Output:
                {
                  "entities":{
                    "date":"next Monday"
                  }
                }
                """
                .formatted(
                        requestType,
                        currentFields,
                        missingFields,
                        customerReply
                );
    }
}
