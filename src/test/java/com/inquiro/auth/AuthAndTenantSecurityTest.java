package com.inquiro.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessProfile;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndTenantSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserAccountJpaRepository users;
    @Autowired AuthSessionJpaRepository sessions;
    @Autowired BusinessMembershipJpaRepository memberships;
    @Autowired BusinessAccountRepository accounts;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetAuthData() {
        sessions.deleteAll();
        memberships.deleteAll();
        users.deleteAll();
    }

    @Test
    void registeredBusinessOwnerCanAccessOnlyTheirBusinessAndPasswordIsHashed() throws Exception {
        String token = registerAndLogin("owner@inquiro.test");
        String created = mvc.perform(post("/api/business/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessName":"Owner Hotel","businessType":"HOSPITALITY","description":"", "services":[]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessId").value(org.hamcrest.Matchers.startsWith("biz_")))
                .andReturn().getResponse().getContentAsString();
        String businessId = mapper.readTree(created).path("businessId").asText();

        UserAccountEntity owner = users.findByEmail("owner@inquiro.test").orElseThrow();
        assertTrue(passwordEncoder.matches("correct horse battery staple", owner.getPasswordHash()));
        assertTrue(memberships.findByUserIdAndBusinessId(owner.getUserId(), businessId)
                .map(member -> member.getRole() == BusinessMembershipRole.OWNER).orElse(false));

        mvc.perform(get("/api/business/accounts/{businessId}/onboarding", businessId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        accounts.save(new BusinessAccount("biz_other_tenant", "Other Hotel",
                new BusinessProfile("Other Hotel", "HOSPITALITY", "", List.of(), null)));
        mvc.perform(get("/api/business/accounts/biz_other_tenant/onboarding")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutRevokesTheOpaqueAccessToken() throws Exception {
        String token = registerAndLogin("logout@inquiro.test");
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/business/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"businessName\":\"Blocked\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanAddAnAdminButAdminCannotManageMemberships() throws Exception {
        String ownerToken = registerAndLogin("membership-owner@inquiro.test");
        String businessResponse = mvc.perform(post("/api/business/accounts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\":\"Membership Hotel\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String businessId = mapper.readTree(businessResponse).path("businessId").asText();

        String adminToken = registerAndLogin("admin@inquiro.test");
        mvc.perform(post("/api/business/accounts/{businessId}/members", businessId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@inquiro.test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mvc.perform(get("/api/business/accounts/{businessId}/onboarding", businessId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/business/accounts/{businessId}/members", businessId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void managementApiStillRejectsUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/business/accounts/biz_001/onboarding"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLogin(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","name":"Test Owner","password":"correct horse battery staple"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correct horse battery staple"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode response = mapper.readTree(login);
        return response.path("accessToken").asText();
    }
}
