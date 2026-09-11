package com.inquiro.knowledge;

import com.inquiro.business.BusinessProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeIngestionController {
    private final KnowledgeIngestionService ingestionService;
    @Value("${inquiro.default-business-id:biz_001}")
    private String defaultBusinessId;

    @PostMapping("/ingest")
    public BusinessProfile ingest(@RequestBody KnowledgeIngestionRequest request) {
        String businessId = request.businessId() == null ? defaultBusinessId : request.businessId();
        // facebookPageId remains accepted for wire compatibility; channel linking is a separate API.
        return ingestionService.ingest(businessId,
                new KnowledgeDocument(request.source(), request.content(), request.metadata()));
    }
}