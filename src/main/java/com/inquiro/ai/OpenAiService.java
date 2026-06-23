package com.inquiro.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.config.OpenAiProperties;
import com.inquiro.inquiry.InquiryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiService implements AiService {

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public InquiryResult analyze(String message) {

        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "OpenAI API key is not configured. Set OPENAI_API_KEY."
            );
        }

        RestClient client = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.getApiKey()
                )
                .build();

        OpenAiRequest request =
                new OpenAiRequest(
                        properties.getModel(),
                        List.of(
                                new Message(
                                        "system",
                                        """
                                                You are an information extraction engine.
                                        
                                                Extract structured information from customer inquiries.
                                        
                                                Return ONLY valid JSON.
                                        
                                                Do not wrap the JSON in markdown code fences.
                                                Do not include explanations.
                                                Do not include any text before or after the JSON.
                                        
                                                Use only these domains:
                                                - HOSPITALITY
                                                - RESTAURANT
                                                - HEALTHCARE
                                                - UNKNOWN
                                        
                                                Use only these services:
                                                - ROOM_BOOKING
                                                - AIRPORT_PICKUP
                                                - TABLE_RESERVATION
                                                - BUFFET_RESERVATION
                                                - DOCTOR_APPOINTMENT
                                                - UNKNOWN
                                        
                                                Use ONLY the following field names.
                                        
                                                ROOM_BOOKING:
                                                - location
                                                - checkInDate
                                                - guestCount
                                                - durationNights
                                        
                                                AIRPORT_PICKUP:
                                                - pickupLocation
                                                - date
                                                - time
                                                - passengerCount
                                        
                                                TABLE_RESERVATION:
                                                - date
                                                - time
                                                - guestCount
                                        
                                                BUFFET_RESERVATION:
                                                - date
                                                - guestCount
                                        
                                                DOCTOR_APPOINTMENT:
                                                - specialty
                                                - date
                                                - timePeriod
                                        
                                                Do not create alternative field names.
                                        
                                                For example:
                                                - use guestCount, not guests
                                                - use specialty, not specialization
                                                - use checkInDate, not arrivalDate
                                                - use durationNights, not checkOutDate
                                                - use durationNights, not departureDate
                                                - use durationNights, not numberOfNights
                                                - use passengerCount, not passengers
                                                - use pickupLocation, not airportLocation
                                        
                                                Never invent fields that are not explicitly mentioned.
                                        
                                                If information is missing, omit the field entirely.
                                        
                                                Do NOT use "UNKNOWN" as a field value.
                                        
                                                If the customer's intent is ambiguous and you cannot determine the service, return:
                                        
                                                {
                                                  "domain": "UNKNOWN",
                                                  "service": "UNKNOWN",
                                                  "fields": {}
                                                }
                                        
                                                Examples:
                                        
                                                Customer:
                                                Need a room near the Eiffel Tower next Friday for 2 adults for 3 nights
                                        
                                                Output:
                                                {
                                                  "domain": "HOSPITALITY",
                                                  "service": "ROOM_BOOKING",
                                                  "fields": {
                                                    "location": "near the Eiffel Tower",
                                                    "checkInDate": "next Friday",
                                                    "guestCount": 2,
                                                    "durationNights": 3
                                                  }
                                                }
                                        
                                                Customer:
                                                Need a room in Paris next Friday for 2 adults
                                        
                                                Output:
                                                {
                                                  "domain": "HOSPITALITY",
                                                  "service": "ROOM_BOOKING",
                                                  "fields": {
                                                    "location": "Paris",
                                                    "checkInDate": "next Friday",
                                                    "guestCount": 2
                                                  }
                                                }
                                        
                                                Customer:
                                                Need airport pickup from Heathrow tomorrow at 10 PM for 4 passengers
                                        
                                                Output:
                                                {
                                                  "domain": "HOSPITALITY",
                                                  "service": "AIRPORT_PICKUP",
                                                  "fields": {
                                                    "pickupLocation": "Heathrow",
                                                    "date": "tomorrow",
                                                    "time": "10 PM",
                                                    "passengerCount": 4
                                                  }
                                                }
                                        
                                                Customer:
                                                Book a table tomorrow for 6 people
                                        
                                                Output:
                                                {
                                                  "domain": "RESTAURANT",
                                                  "service": "TABLE_RESERVATION",
                                                  "fields": {
                                                    "date": "tomorrow",
                                                    "guestCount": 6
                                                  }
                                                }
                                        
                                                Customer:
                                                Book a table tomorrow at 7 PM for 6 people
                                        
                                                Output:
                                                {
                                                  "domain": "RESTAURANT",
                                                  "service": "TABLE_RESERVATION",
                                                  "fields": {
                                                    "date": "tomorrow",
                                                    "time": "7 PM",
                                                    "guestCount": 6
                                                  }
                                                }
                                        
                                                Customer:
                                                Need buffet tomorrow for 8 people
                                        
                                                Output:
                                                {
                                                  "domain": "RESTAURANT",
                                                  "service": "BUFFET_RESERVATION",
                                                  "fields": {
                                                    "date": "tomorrow",
                                                    "guestCount": 8
                                                  }
                                                }
                                        
                                                Customer:
                                                Need appointment with a cardiologist tomorrow morning
                                        
                                                Output:
                                                {
                                                  "domain": "HEALTHCARE",
                                                  "service": "DOCTOR_APPOINTMENT",
                                                  "fields": {
                                                    "specialty": "CARDIOLOGY",
                                                    "date": "tomorrow",
                                                    "timePeriod": "morning"
                                                  }
                                                }
                                        
                                                Customer:
                                                Can I see a dentist this weekend?
                                        
                                                Output:
                                                {
                                                  "domain": "HEALTHCARE",
                                                  "service": "DOCTOR_APPOINTMENT",
                                                  "fields": {
                                                    "specialty": "DENTISTRY",
                                                    "date": "this weekend"
                                                  }
                                                }
                                        
                                                Customer:
                                                Need a booking tomorrow
                                        
                                                Output:
                                                {
                                                  "domain": "UNKNOWN",
                                                  "service": "UNKNOWN",
                                                  "fields": {}
                                                }
                                        
                                                If the customer asks for multiple services in one message, return the strongest primary service for now.
                                        
                                                Return JSON only.
                                        
                                        
                                        """
                                ),
                                new Message(
                                        "user",
                                        message
                                )
                        )
                );

        OpenAiResponse response =
                client.post()
                        .uri("/v1/chat/completions")
                        .body(request)
                        .retrieve()
                        .body(OpenAiResponse.class);

        if (response == null ||
                response.choices() == null ||
                response.choices().isEmpty()) {

            throw new IllegalStateException(
                    "OpenAI response did not include choices."
            );
        }

        String assistantContent = response
                .choices()
                .get(0)
                .message()
                .content();

        System.out.println("AI Response:");
        System.out.println(assistantContent);

        try {
            return objectMapper.readValue(
                    assistantContent,
                    InquiryResult.class
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse AI response: " + assistantContent,
                    e
            );
        }
    }


    @Override
    public RequestAnalysis analyzeRequest(
            String message) {

        RestClient client = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.getApiKey()
                )
                .build();

        OpenAiRequest request =
                new OpenAiRequest(
                        properties.getModel(),
                        List.of(
                                new Message(
                                        "system",
                                        RequestAnalysisPrompt.systemPrompt()
                                ),
                                new Message(
                                        "user",
                                        message
                                )
                        )
                );

        OpenAiResponse response =
                client.post()
                        .uri("/v1/chat/completions")
                        .body(request)
                        .retrieve()
                        .body(OpenAiResponse.class);

        if (response == null ||
                response.choices() == null ||
                response.choices().isEmpty()) {

            throw new IllegalStateException(
                    "OpenAI response did not include choices."
            );
        }

        String assistantContent =
                response.choices()
                        .get(0)
                        .message()
                        .content();

        System.out.println(
                "Request Analysis Response:"
        );

        System.out.println(
                assistantContent
        );

        try {

            return objectMapper.readValue(
                    assistantContent,
                    RequestAnalysis.class
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to parse request analysis: "
                            + assistantContent,
                    e
            );
        }
    }

    @Override
    public FollowUpAnalysis analyzeFollowUp(
            String requestType,
            Map<String, Object> currentFields,
            List<String> missingFields,
            String message) {

        RestClient client = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.getApiKey()
                )
                .build();

        OpenAiRequest request =
                new OpenAiRequest(
                        properties.getModel(),
                        List.of(
                                new Message(
                                        "system",
                                        RequestFollowUpPrompt.build(
                                                requestType,
                                                currentFields,
                                                missingFields,
                                                message
                                        )
                                )
                        )
                );

        OpenAiResponse response =
                client.post()
                        .uri("/v1/chat/completions")
                        .body(request)
                        .retrieve()
                        .body(OpenAiResponse.class);

        if (response == null ||
                response.choices() == null ||
                response.choices().isEmpty()) {

            throw new IllegalStateException(
                    "OpenAI response did not include choices."
            );
        }

        String assistantContent =
                response.choices()
                        .get(0)
                        .message()
                        .content();

        System.out.println(
                "Follow Up Analysis Response:"
        );

        System.out.println(
                assistantContent
        );

        try {

            return objectMapper.readValue(
                    assistantContent,
                    FollowUpAnalysis.class
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to parse follow-up analysis: "
                            + assistantContent,
                    e
            );
        }
    }
}