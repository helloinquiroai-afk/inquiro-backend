package com.inquiro.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.business.BusinessProfile;
import com.inquiro.business.BusinessProfileProvider;
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
    private final BusinessProfileProvider businessProfileProvider;

    @Override
    public InquiryResult analyze(String message) {

        RequestAnalysis analysis =
                analyzeRequest(message);

        BusinessProfile businessProfile =
                businessProfileProvider.get();

        return new InquiryResult(
                businessProfile.businessType(),
                analysis.intent(),
                analysis.entities()
        );
    }


    @Override
    public RequestAnalysis analyzeRequest(
            String message) {

        return analyzeRequest(
                message,
                businessProfileProvider.get()
        );
    }

    @Override
    public RequestAnalysis analyzeRequest(
            String message,
            BusinessProfile businessProfile) {

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
                                        RequestAnalysisPrompt.systemPrompt(businessProfile)
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

    @Override
    public ConversationIntentAnalysis analyzeConversationIntent(
            String service,
            Map<String, Object> currentFields,
            List<String> missingFields,
            String message) {

        return analyzeConversationIntent(
                businessProfileProvider.get(),
                service,
                currentFields,
                missingFields,
                message
        );
    }

    @Override
    public ConversationIntentAnalysis analyzeConversationIntent(
            BusinessProfile businessProfile,
            String service,
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
                                        ConversationIntentPrompt.build(
                                                businessProfile,
                                                service,
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
                "Conversation Intent Response:"
        );

        System.out.println(
                assistantContent
        );

        try {

            return objectMapper.readValue(
                    assistantContent,
                    ConversationIntentAnalysis.class
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to parse conversation intent: "
                            + assistantContent,
                    e
            );
        }
    }

    @Override
    public String answerBusinessQuestion(
            String customerQuestion,
            BusinessProfile businessProfile) {

        String prompt = BusinessQuestionPrompt.build(
                customerQuestion,
                businessProfile
        );

        // Use your existing OpenAI call mechanism here.
        return callOpenAi(prompt);
    }

    private String callOpenAi(String prompt) {

        if (properties.getApiKey() == null ||
                properties.getApiKey().isBlank()) {

            throw new IllegalStateException(
                    "OpenAI API key is not configured. Set OPENAI_API_KEY."
            );
        }

        RestClient client =
                RestClient.builder()
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
                                        prompt
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

        String content =
                response.choices()
                        .get(0)
                        .message()
                        .content();

        System.out.println("Business Question Response:");
        System.out.println(content);

        return content;
    }
}
