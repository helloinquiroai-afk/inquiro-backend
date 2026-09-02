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
                You are an information extraction engine for a multi-turn conversation.

                Current Request Type:
                %s
                
                Known Information:
                %s
                
                Missing Information (in order):
                %s
                
                Customer Reply:
                %s
                
                Your task is to extract ONLY information that fills the missing fields.
                
                IMPORTANT RULES
                
                1. Never modify existing known information.
                
                2. Only populate fields listed under "Missing Information".
                
                3. If the customer provides a short numeric reply (for example "2", "3", "5")
                   and multiple numeric fields are still missing,
                   assume the number belongs to the FIRST missing field unless the customer
                   explicitly says otherwise.
                
                4. DATE HANDLING:
                
                   Preserve relative date expressions exactly as provided by the customer.
                   
                   Do NOT calculate, resolve, or invent a calendar date.
                   
                   Examples:
                   
                   Customer Reply:
                   tomorrow
                   
                   Output:
                   {
                     "entities": {
                       "checkInDate": "tomorrow"
                     }
                   }
                   
                   Customer Reply:
                   next Friday
                   
                   Output:
                   {
                     "entities": {
                       "checkInDate": "next Friday"
                     }
                   }
                   
                   Customer Reply:
                   this weekend
                   
                   Output:
                   {
                     "entities": {
                       "checkInDate": "this weekend"
                     }
                   }
                   
                   Customer Reply:
                   Monday
                   
                   Output:
                   {
                     "entities": {
                       "checkInDate": "Monday"
                     }
                   }
                   
                   NEVER convert:
                   
                   "next Friday"
                   
                   into:
                   
                   "2024-06-07"
                   
                   or any other YYYY-MM-DD date.
                   
                   The application will resolve relative dates later.
                
                Example:
                
                Missing Information:
                durationNights
                guestCount
                
                Customer Reply:
                2
                
                Output:
                {
                  "entities": {
                    "durationNights": 2
                  }
                }
                
                Example:
                
                Missing Information:
                guestCount
                
                Customer Reply:
                2
                
                Output:
                {
                  "entities": {
                    "guestCount": 2
                  }
                }
                
                Example:
                
                Missing Information:
                guestCount
                durationNights
                
                Customer Reply:
                2 adults
                
                Output:
                {
                  "entities": {
                    "guestCount": 2
                  }
                }
                
                Example:
                
                Missing Information:
                durationNights
                
                Customer Reply:
                2 nights
                
                Output:
                {
                  "entities": {
                    "durationNights": 2
                  }
                }
                
                Return ONLY valid JSON.
                
                {
                  "entities": {}
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