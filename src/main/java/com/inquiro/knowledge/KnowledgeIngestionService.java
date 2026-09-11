package com.inquiro.knowledge;

import com.inquiro.business.BusinessProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeIngestionService {

    private final BusinessKnowledgeExtractor extractor;
    private final BusinessKnowledgeStore store;

    public BusinessProfile ingest(
            String businessId,
            KnowledgeDocument document) {

        BusinessKnowledgeService.validateBusinessId(businessId);
        if (document == null || document.content().isBlank() || document.content().length() > 64000) {
            throw new IllegalArgumentException("Nonempty knowledge content is required");
        }

        BusinessProfile profile =
                extractor.extract(document);

        store.save(
                businessId,
                profile
        );

        return store.findByBusinessId(businessId);
    }
}
