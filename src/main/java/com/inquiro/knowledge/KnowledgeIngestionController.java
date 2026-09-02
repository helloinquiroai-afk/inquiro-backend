package com.inquiro.knowledge;

import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeIngestionController {

    private final KnowledgeIngestionService ingestionService;
    private final BusinessAccountRepository businessAccountRepository;

    @PostMapping("/ingest")
    public BusinessProfile ingest(
            @RequestBody KnowledgeIngestionRequest request) {

        String businessId =
                request.businessId() == null ||
                        request.businessId().isBlank()
                        ? "biz_001"
                        : request.businessId();

        BusinessProfile profile =
                ingestionService.ingest(
                        businessId,
                        new KnowledgeDocument(
                                request.source(),
                                request.content(),
                                request.metadata() == null
                                        ? Map.of()
                                        : request.metadata()
                        )
                );

        if (request.facebookPageId() != null &&
                !request.facebookPageId().isBlank()) {

            businessAccountRepository.save(
                    new BusinessAccount(
                            businessId,
                            profile.businessName(),
                            request.facebookPageId(),
                            profile
                    )
            );
        }

        return profile;
    }
}
