package com.inquiro.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.ai.AiService;
import com.inquiro.ai.BusinessQuestionPrompt;
import com.inquiro.business.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BusinessKnowledgeApiTest {
    private static final String BASE = "/api/business/accounts/knowledge-test/knowledge";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired BusinessAccountRepository accounts;
    @Autowired BusinessAccountJpaRepository jpa;
    @Autowired BusinessKnowledgeService knowledge;
    @Autowired BusinessKnowledgeStore store;
    @Autowired KnowledgeIngestionService ingestion;
    @MockitoBean AiService ai;

    @BeforeEach void setup() {
        accounts.save(new BusinessAccount("knowledge-test", "Paris Hotel", KnowledgeFixtures.profile()));
    }

    @Test void readsPersistedKnowledge() throws Exception {
        mvc.perform(get(BASE).header("X-Inquiro-Management-Key", "test-management-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.facts.parking").value("Yes, free parking is available for hotel guests."))
                .andExpect(jsonPath("$.policies[0]").value("Cancellation requires 24 hours notice."));
    }

    @Test void replacesOnlyKnowledgeAndReloadsAllFieldsFromDatabase() throws Exception {
        var original = KnowledgeFixtures.profile();
        var replacement = original.knowledge().withFaqs(List.of("Q: Is breakfast included?\nA: Breakfast is included."));
        mvc.perform(put(BASE).header("X-Inquiro-Management-Key", "test-management-key")
                .contentType("application/json").content(mapper.writeValueAsString(replacement)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.faqs[0]").value(replacement.faqs().get(0)));
        // A new repository adapter reads from JPA; no knowledge-store cache can satisfy this.
        var reloaded = new JpaBusinessAccountRepository(jpa, new ObjectMapper()).findByBusinessId("knowledge-test");
        assertEquals(replacement, reloaded.profile().knowledge());
        assertEquals(original.services(), reloaded.profile().services());
        assertEquals(original.description(), reloaded.profile().description());
        assertEquals(original.businessName(), reloaded.profile().businessName());
        assertEquals(replacement, store.findByBusinessId("knowledge-test").knowledge());
    }

    @Test void fullReplacementNormalizesNullCollectionsAndExplicitlyClearsOmittedSections() throws Exception {
        mvc.perform(put(BASE).header("X-Inquiro-Management-Key", "test-management-key")
                .contentType("application/json").content("{\"faqs\":null,\"facts\":null,\"boundaries\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.faqs").isEmpty())
                .andExpect(jsonPath("$.facts").isEmpty()).andExpect(jsonPath("$.boundaries.supported").isEmpty());
        assertEquals(KnowledgeFixtures.profile().services(), accounts.findByBusinessId("knowledge-test").profile().services());
    }

    @Test void missingBusinessReturns404AndNeverCreatesAccount() throws Exception {
        String absent = "/api/business/accounts/does-not-exist/knowledge";
        mvc.perform(get(absent).header("X-Inquiro-Management-Key", "test-management-key")).andExpect(status().isNotFound());
        mvc.perform(put(absent).header("X-Inquiro-Management-Key", "test-management-key").contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(absent + "/faq-suggestions").header("X-Inquiro-Management-Key", "test-management-key"))
                .andExpect(status().isNotFound());
        assertNull(accounts.findByBusinessId("does-not-exist"));
        verifyNoInteractions(ai);
    }

    @Test void invalidIdsAndPayloadsReturn400WithoutChangingKnowledge() throws Exception {
        mvc.perform(get("/api/business/accounts/bad!id/knowledge").header("X-Inquiro-Management-Key", "test-management-key"))
                .andExpect(status().isBadRequest());
        for (String body : List.of("null", "[]", "{", "{\"faqz\":[]}", "{\"faqs\":[null]}",
                "{\"facts\":{\"parking\":null}}", "{\"facts\":{\"parking\":\" \"}}", "{\"faqs\":[\" \"]}")) {
            mvc.perform(put(BASE).header("X-Inquiro-Management-Key", "test-management-key")
                    .contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
        }
        assertEquals(KnowledgeFixtures.profile().knowledge(), knowledge.get("knowledge-test"));
    }

    @Test void allManagementActionsRequireExistingOperatorKey() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(put(BASE).contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/faq-suggestions")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/faq-suggestions/review").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(ai);
    }

    @Test void suggestionsStayTransientUntilExplicitEditedApproval() throws Exception {
        var original = knowledge.get("knowledge-test");
        when(ai.suggestFaqs(any())).thenReturn(List.of(new FaqSuggestion("Is parking free?", "Parking is free.")));
        mvc.perform(post(BASE + "/faq-suggestions").header("X-Inquiro-Management-Key", "test-management-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].question").value("Is parking free?"));
        assertEquals(original, knowledge.get("knowledge-test"));
        String review = """
                {"decision":"APPROVE","suggestion":{"question":"Is guest parking free?","answer":"Guest parking is free."}}
                """;
        for (int i = 0; i < 2; i++) {
            mvc.perform(post(BASE + "/faq-suggestions/review").header("X-Inquiro-Management-Key", "test-management-key")
                    .contentType("application/json").content(review)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.decision").value("APPROVE"));
        }
        var approved = knowledge.get("knowledge-test");
        assertEquals(original.faqs().size() + 1, approved.faqs().size());
        assertEquals(original.facts(), approved.facts());
        assertTrue(BusinessQuestionPrompt.sources(accounts.findByBusinessId("knowledge-test").profile()).stream()
                .anyMatch(source -> source.answer().equals("Guest parking is free.")));
    }

    @Test void explicitRejectionDoesNotPublishOrAlterKnowledge() throws Exception {
        var original = knowledge.get("knowledge-test");
        mvc.perform(post(BASE + "/faq-suggestions/review").header("X-Inquiro-Management-Key", "test-management-key")
                .contentType("application/json").content("""
                    {"decision":"REJECT","suggestion":{"question":"Is the pool heated?","answer":"The pool is heated."}}
                    """)).andExpect(status().isOk()).andExpect(jsonPath("$.decision").value("REJECT"));
        assertEquals(original, knowledge.get("knowledge-test"));
        assertFalse(BusinessQuestionPrompt.sources(accounts.findByBusinessId("knowledge-test").profile()).stream()
                .anyMatch(source -> source.answer().contains("heated")));
    }

    @Test void reviewValidationAndAiFailureAreSafe() throws Exception {
        mvc.perform(post(BASE + "/faq-suggestions/review").header("X-Inquiro-Management-Key", "test-management-key")
                .contentType("application/json").content("{\"decision\":\"APPROVE\",\"suggestion\":{\"question\":\"Q\",\"answer\":\"\"}}"))
                .andExpect(status().isBadRequest());
        when(ai.suggestFaqs(any())).thenThrow(new IllegalStateException("secret-key upstream failure"));
        mvc.perform(post(BASE + "/faq-suggestions").header("X-Inquiro-Management-Key", "test-management-key"))
                .andExpect(status().isBadGateway()).andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret-key"))));
    }

    @Test void fullKnowledgePutCanRemoveFaqsWithoutChangingOtherSections() throws Exception {
        var original = knowledge.get("knowledge-test");
        knowledge.replace("knowledge-test", original.withFaqs(List.of()));
        assertTrue(knowledge.get("knowledge-test").faqs().isEmpty());
        assertEquals(original.facts(), knowledge.get("knowledge-test").facts());
        assertEquals(original.boundaries(), knowledge.get("knowledge-test").boundaries());
    }

    @Test void ingestionPersistsWithoutFacebookIdAndDoesNotEraseExistingSections() throws Exception {
        mvc.perform(post("/api/knowledge/ingest").header("X-Inquiro-Management-Key", "test-management-key")
                .contentType("application/json").content(mapper.writeValueAsString(Map.of(
                        "businessId", "knowledge-test", "source", "TEXT", "content", "Phone: \nEmail: desk@example.test"))))
                .andExpect(status().isOk());
        var saved = accounts.findByBusinessId("knowledge-test").profile();
        assertEquals("Paris Hotel", saved.businessName());
        assertEquals(KnowledgeFixtures.profile().services(), saved.services());
        assertEquals(KnowledgeFixtures.profile().knowledge().faqs(), saved.knowledge().faqs());
        assertEquals("+33123456789", saved.knowledge().contactInformation().get("phone"));
        assertEquals("desk@example.test", saved.knowledge().contactInformation().get("email"));
        assertEquals(KnowledgeFixtures.profile().knowledge().instructions(), saved.knowledge().instructions());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> ingestion.ingest("absent-ingestion", KnowledgeDocument.manualForm("Phone: 123")));
        assertNull(accounts.findByBusinessId("absent-ingestion"));
    }

    @Test void defaultProfileProviderReadsLatestPersistedKnowledge() {
        var provider = new BusinessProfileProvider(new BusinessKnowledgeExtractor(), accounts, "knowledge-test");
        assertEquals(KnowledgeFixtures.profile().knowledge(), provider.get().knowledge());
        knowledge.replace("knowledge-test", BusinessKnowledge.empty());
        assertEquals(BusinessKnowledge.empty(), provider.get().knowledge());
    }

    @Test void concurrentFaqApprovalsDoNotLoseOneAnother() throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var start = new java.util.concurrent.CountDownLatch(1);
            var a = pool.submit(() -> { start.await(); return knowledge.review("knowledge-test", BusinessKnowledgeService.Decision.APPROVE, new FaqSuggestion("Question A?", "Answer A.")); });
            var b = pool.submit(() -> { start.await(); return knowledge.review("knowledge-test", BusinessKnowledgeService.Decision.APPROVE, new FaqSuggestion("Question B?", "Answer B.")); });
            start.countDown();
            a.get(); b.get();
            assertEquals(3, knowledge.get("knowledge-test").faqs().size());
        } finally { pool.shutdownNow(); }
    }

    @Test void businessKnowledgeIsIsolated() {
        accounts.save(new BusinessAccount("knowledge-other", "Other", KnowledgeFixtures.profile()));
        knowledge.replace("knowledge-test", BusinessKnowledge.empty());
        assertEquals(KnowledgeFixtures.profile().knowledge(), knowledge.get("knowledge-other"));
    }
}
