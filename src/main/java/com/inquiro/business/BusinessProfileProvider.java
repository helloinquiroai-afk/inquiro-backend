package com.inquiro.business;

import com.inquiro.knowledge.BusinessKnowledgeExtractor;
import com.inquiro.knowledge.KnowledgeDocument;
import org.springframework.stereotype.Component;

@Component
public class BusinessProfileProvider {

    private final BusinessKnowledgeExtractor extractor;
    private BusinessProfile defaultProfile;

    public BusinessProfileProvider(
            BusinessKnowledgeExtractor extractor) {

        this.extractor = extractor;
    }

    public BusinessProfileProvider() {
        this(new BusinessKnowledgeExtractor());
    }

    public BusinessProfile get() {

        if (defaultProfile == null) {
            defaultProfile =
                    extractor.extract(
                            KnowledgeDocument.manualForm(
                                    defaultOwnerInformation()
                            )
                    );
        }

        return defaultProfile;
    }

    private String defaultOwnerInformation() {

        return """
                Business name:
                ABC Auto Care

                Business type:
                Vehicle Service Center

                Description:
                Vehicle maintenance and inspection services for cars, vans, and SUVs.

                Services:
                - Oil change
                - Brake repair
                - AC repair
                - Vehicle inspection

                Vehicles:
                - Cars
                - Vans
                - SUVs

                Opening hours:
                Monday-Saturday 8 AM - 6 PM

                Booking:
                Required

                Cancellation:
                24 hours notice

                Phone:
                071xxxxxxx

                Not supported:
                - Engine replacement
                - Body painting

                Requires human:
                - Custom engine work
                - Insurance claims
                """;
    }
}
