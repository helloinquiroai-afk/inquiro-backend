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

        BusinessProfile profile =
                extractor.extract(document);

        store.save(
                businessId,
                profile
        );

        return profile;
    }
}
