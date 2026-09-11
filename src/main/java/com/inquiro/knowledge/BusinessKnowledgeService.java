package com.inquiro.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.ai.AiService;
import com.inquiro.business.*;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BusinessKnowledgeService {
    private final BusinessAccountRepository accounts;
    private final AiService ai;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public BusinessKnowledge get(String businessId) {
        return account(businessId, false).profile().knowledge();
    }

    /** Explicit complete replacement of knowledge; identity and configured workflows stay intact. */
    @Transactional
    public BusinessKnowledge replace(String businessId, BusinessKnowledge knowledge) {
        var account = account(businessId, true);
        validate(knowledge);
        save(account, knowledge);
        return knowledge;
    }

    public List<FaqSuggestion> suggest(String businessId) {
        var profile = account(businessId, false).profile();
        try {
            var suggestions = ai.suggestFaqs(profile);
            if (suggestions == null || suggestions.size() > 10) throw new IllegalStateException();
            for (var suggestion : suggestions) validateFaq(suggestion);
            return List.copyOf(suggestions);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FAQ suggestions are temporarily unavailable");
        }
    }

    @Transactional
    public BusinessKnowledge review(String businessId, Decision decision, FaqSuggestion suggestion) {
        var account = account(businessId, true);
        if (decision == null) throw new IllegalArgumentException("Review decision is required");
        validateFaq(suggestion);
        var knowledge = account.profile().knowledge();
        if (decision == Decision.REJECT) return knowledge;
        var faqs = new ArrayList<>(knowledge.faqs());
        String approved = suggestion.asApprovedFaq();
        if (!faqs.contains(approved)) faqs.add(approved);
        var replacement = knowledge.withFaqs(faqs);
        validate(replacement);
        save(account, replacement);
        return replacement;
    }

    private void save(BusinessAccount account, BusinessKnowledge knowledge) {
        accounts.save(new BusinessAccount(account.businessId(), account.businessName(),
                account.profile().withKnowledge(knowledge)));
    }

    private BusinessAccount account(String id, boolean lock) {
        validateBusinessId(id);
        var account = lock ? accounts.findByBusinessIdForUpdate(id) : accounts.findByBusinessId(id);
        if (account == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        return account;
    }

    public static void validateBusinessId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}")) {
            throw new IllegalArgumentException("Invalid business ID");
        }
    }

    public void validate(BusinessKnowledge knowledge) {
        if (knowledge == null) throw new IllegalArgumentException("Knowledge is required");
        JsonNode tree = mapper.valueToTree(knowledge);
        if (tree.toString().length() > 64000) throw new IllegalArgumentException("Knowledge is too large");
        validateNode(tree, false);
    }

    private void validateNode(JsonNode node, boolean entry) {
        if (node.isNull()) {
            if (entry) throw new IllegalArgumentException("Knowledge entries must not be null");
        } else if (node.isTextual()) {
            if (node.asText().length() > 5000 || (entry && node.asText().isBlank())) {
                throw new IllegalArgumentException("Invalid knowledge text");
            }
        } else if (node.isContainerNode()) {
            if (node.size() > 100) throw new IllegalArgumentException("Too many knowledge entries");
            node.fields().forEachRemaining(field -> {
                if (field.getKey().isBlank() || field.getKey().length() > 200) {
                    throw new IllegalArgumentException("Invalid knowledge key");
                }
            });
            for (JsonNode child : node) validateNode(child, node.isArray() || entry || child.isObject());
        } else throw new IllegalArgumentException("Knowledge must contain text");
    }

    private static void validateFaq(FaqSuggestion faq) {
        if (faq == null || faq.question() == null || faq.question().isBlank() || faq.question().length() > 500
                || faq.answer() == null || faq.answer().isBlank() || faq.answer().length() > 4000) {
            throw new IllegalArgumentException("A question and answer are required");
        }
    }

    public enum Decision { APPROVE, REJECT }
}
