package com.inquiro.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.business.BusinessProfileProvider;
import com.inquiro.config.OpenAiProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenAiServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private OpenAiService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder().baseUrl("https://api.openai.com");
        server = MockRestServiceServer.bindTo(builder).build();
        var properties = new OpenAiProperties();
        properties.setApiKey("test-only");
        service = new OpenAiService(properties, mapper, new BusinessProfileProvider(), builder.build());
    }

    private void response(String content) throws Exception {
        String json = mapper.writeValueAsString(Map.of("choices",
                List.of(Map.of("message", Map.of("role", "assistant", "content", content)))));
        server.expect(requestTo("https://api.openai.com/v1/chat/completions")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test void parsesRequestEntities() throws Exception {
        response("""
                {"intent":"ROOM_BOOKING","confidence":0.98,"entities":{"location":"Paris"}}
                """);
        var result = service.analyzeRequest("I need a hotel in Paris");
        assertEquals("ROOM_BOOKING", result.intent());
        assertEquals("Paris", result.entities().get("location"));
        server.verify();
    }

    @Test void parsesMultipleFollowUpEntitiesAndCorrections() throws Exception {
        response("""
                {"entities":{"checkInDate":"next Friday","guestCount":3,"durationNights":3}}
                """);
        var result = service.analyzeFollowUp("ROOM_BOOKING", Map.of("guestCount", 2),
                List.of("checkInDate", "durationNights"), "Next Friday, actually three adults for three nights");
        assertEquals(3, result.entities().size());
        assertEquals(3, result.entities().get("guestCount"));
        server.verify();
    }

    @Test void parsesConversationIntent() throws Exception {
        response("""
                {"intent":"NEW_REQUEST","confidence":0.95}
                """);
        assertEquals("NEW_REQUEST", service.analyzeConversationIntent("ROOM_BOOKING", Map.of(),
                List.of("date"), "Do you have parking?").intent());
    }

    @Test void rejectsMalformedJsonWithoutEchoingItInErrorMessage() throws Exception {
        response("private customer data is not JSON");
        var error = assertThrows(IllegalStateException.class, () -> service.analyzeRequest("hi"));
        assertFalse(error.getMessage().contains("private customer"));
    }

    @Test void rejectsEmptyChoices() {
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class, () -> service.analyzeRequest("hi"));
    }

    @Test void surfacesHttpFailureToChannelErrorHandler() {
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThrows(org.springframework.web.client.RestClientResponseException.class,
                () -> service.analyzeRequest("hi"));
    }

    @Test void returnsBusinessKnowledgeAnswer() throws Exception {
        var profile = new BusinessProfileProvider().get();
        profile = profile.withKnowledge(new com.inquiro.business.BusinessKnowledge("", List.of(), List.of(),
                Map.of("parking", "Yes, parking is free for hotel guests."), List.of(), List.of(), ""));
        response("{\"sourceIds\":[\"fact:parking\"],\"missingInformation\":false}");
        assertEquals("Yes, parking is free for hotel guests.",
                service.answerBusinessQuestion("Is parking free?", profile));
    }
}
