package com.inquiro.knowledge;

import com.inquiro.business.*;
import com.inquiro.request.RequestDefinition;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Compatibility facade over the persisted account profile; no independent knowledge cache. */
@Repository
@RequiredArgsConstructor
public class BusinessKnowledgeStore {
    private final BusinessAccountRepository accounts;
    private final BusinessKnowledgeService knowledgeService;

    @Transactional
    public void save(String businessId, BusinessProfile extracted) {
        BusinessKnowledgeService.validateBusinessId(businessId);
        var account = accounts.findByBusinessIdForUpdate(businessId);
        if (account == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        if (extracted == null) throw new IllegalArgumentException("Profile is required");
        var current = account.profile();
        var a = current.knowledge();
        var b = extracted.knowledge();
        var knowledge = new BusinessKnowledge(
                text(b.businessDescription(), a.businessDescription()),
                union(a.services(), b.services()), union(a.products(), b.products()), merge(a.facts(), b.facts()),
                union(a.faqs(), b.faqs()), union(a.policies(), b.policies()),
                text(a.instructions(), b.instructions()), // extractor defaults must not erase operator instructions
                merge(a.operatingHours(), b.operatingHours()), union(a.locations(), b.locations()),
                merge(a.contactInformation(), b.contactInformation()), merge(a.bookingRules(), b.bookingRules()),
                union(a.capabilities(), b.capabilities()), union(a.restrictions(), b.restrictions()),
                new BusinessBoundaries(union(a.boundaries().supported(), b.boundaries().supported()),
                        union(a.boundaries().notSupported(), b.boundaries().notSupported()),
                        union(a.boundaries().requiresHuman(), b.boundaries().requiresHuman())));
        knowledgeService.validate(knowledge);
        Map<String, RequestDefinition> services = new LinkedHashMap<>();
        current.services().forEach(service -> services.put(service.requestType(), service));
        extracted.services().forEach(service -> services.putIfAbsent(service.requestType(), service));
        String name = "Unnamed Business".equals(extracted.businessName()) ? current.businessName()
                : text(extracted.businessName(), current.businessName());
        String type = "GENERAL_BUSINESS".equals(extracted.businessType()) ? current.businessType()
                : text(extracted.businessType(), current.businessType());
        var profile = new BusinessProfile(name, type, text(extracted.description(), current.description()),
                List.copyOf(services.values()), knowledge);
        accounts.save(new BusinessAccount(businessId, name, profile));
    }

    public BusinessProfile findByBusinessId(String businessId) {
        var account = accounts.findByBusinessId(businessId);
        return account == null ? null : account.profile();
    }

    private static String text(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate;
    }

    private static List<String> union(List<String> current, List<String> incoming) {
        var result = new LinkedHashSet<>(current);
        incoming.stream().filter(value -> value != null && !value.isBlank()).forEach(result::add);
        return List.copyOf(result);
    }

    private static Map<String, String> merge(Map<String, String> current, Map<String, String> incoming) {
        var result = new LinkedHashMap<>(current);
        incoming.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) result.put(key, value);
        });
        return Map.copyOf(result);
    }
}