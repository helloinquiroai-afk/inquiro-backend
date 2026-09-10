package com.inquiro.conversation;

import com.inquiro.business.*;
import com.inquiro.communication.messenger.MetaSignatureValidator;
import com.inquiro.config.ManagementAccessFilter;
import com.inquiro.inquiry.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WebsiteCompatibilityTest {
    private MockMvc mvc;
    private ConversationService service;
    private ConversationRepository conversations;
    private BusinessAccountRepository accounts;

    @BeforeEach void setup() {
        service = mock(ConversationService.class);
        conversations = mock(ConversationRepository.class);
        accounts = mock(BusinessAccountRepository.class);
        var channels = mock(BusinessChannelRepository.class);
        when(channels.findByTypeAndExternalId(BusinessChannelType.WEBSITE, "site"))
                .thenReturn(new BusinessChannel("channel", "business", BusinessChannelType.WEBSITE, "site", true));
        var controller = new ConversationController(service, conversations, channels);
        ReflectionTestUtils.setField(controller, "websiteChannelId", "site");
        mvc = MockMvcBuilders.standaloneSetup(controller, new BusinessProfileController(accounts))
                .addFilters(new ManagementAccessFilter("operator-key", new MetaSignatureValidator())).build();
    }

    @Test void keepsExistingWebsiteMessagePayloadAndResponseShape() throws Exception {
        when(service.process("browser-session", BusinessChannelType.WEBSITE, "site", "Hello"))
                .thenReturn(new InquiryResponse(new InquiryResult("HOSPITALITY", "GREETING", Map.of()),
                        List.of(), InquiryStatus.INFORMATION_COLLECTED, "Hello!"));
        mvc.perform(post("/api/conversations/message").contentType("application/json")
                .content("{\"sessionId\":\"browser-session\",\"message\":\"Hello\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reply").value("Hello!"))
                .andExpect(jsonPath("$.inquiry.service").value("GREETING"));
    }

    @Test void resetCannotDeleteMessengerSessionWithSameCustomerId() throws Exception {
        mvc.perform(delete("/api/conversations/browser-session")).andExpect(status().isOk());
        verify(conversations).remove(new ConversationIdentity("business", BusinessChannelType.WEBSITE,
                "site", "browser-session").sessionId());
        verify(conversations, never()).remove("browser-session");
    }

    @Test void rejectsBlankWebsiteInputBeforeAi() throws Exception {
        mvc.perform(post("/api/conversations/message").contentType("application/json")
                .content("{\"sessionId\":\"\",\"message\":\" \"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void managementApisRequireOperatorKey() throws Exception {
        mvc.perform(get("/api/business/accounts/business/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/business/accounts/business/profile").header("X-Inquiro-Management-Key", "wrong"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(accounts);
    }

    @Test void configuredOperatorCanReadProfile() throws Exception {
        when(accounts.findByBusinessId("business")).thenReturn(new BusinessAccount("business", "Hotel",
                new BusinessProfile("Hotel", "HOSPITALITY", "", List.of(), null)));
        mvc.perform(get("/api/business/accounts/business/profile").header("X-Inquiro-Management-Key", "operator-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.businessName").value("Hotel"));
    }
}