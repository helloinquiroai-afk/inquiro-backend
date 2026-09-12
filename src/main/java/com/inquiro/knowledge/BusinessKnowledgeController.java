package com.inquiro.knowledge;

import com.inquiro.business.BusinessKnowledge;
import com.inquiro.auth.TenantAuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business/accounts/{businessId}/knowledge")
@RequiredArgsConstructor
public class BusinessKnowledgeController {
    private final BusinessKnowledgeService service;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;
    private final TenantAuthorizationService tenantAuthorization;

    @GetMapping
    public BusinessKnowledge get(@PathVariable String businessId) {
        tenantAuthorization.requireBusinessAccess(businessId);
        return service.get(businessId);
    }

    @PutMapping
    public BusinessKnowledge replace(@PathVariable String businessId, @RequestBody com.fasterxml.jackson.databind.JsonNode request)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        tenantAuthorization.requireBusinessAccess(businessId);
        BusinessKnowledge knowledge = mapper.readerFor(BusinessKnowledge.class)
                .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(request.toString());
        return service.replace(businessId, knowledge);
    }

    @PostMapping("/faq-suggestions")
    public List<FaqSuggestion> suggest(@PathVariable String businessId) {
        tenantAuthorization.requireBusinessAccess(businessId);
        return service.suggest(businessId);
    }

    @PostMapping("/faq-suggestions/review")
    public ReviewResponse review(@PathVariable String businessId, @Valid @RequestBody ReviewRequest request) {
        tenantAuthorization.requireBusinessAccess(businessId);
        var knowledge = service.review(businessId, request.decision(), request.suggestion());
        return new ReviewResponse(request.decision(), knowledge);
    }

    public record ReviewRequest(@NotNull BusinessKnowledgeService.Decision decision,
                                @NotNull @Valid FaqSuggestion suggestion) {}
    public record ReviewResponse(BusinessKnowledgeService.Decision decision, BusinessKnowledge knowledge) {}
}
