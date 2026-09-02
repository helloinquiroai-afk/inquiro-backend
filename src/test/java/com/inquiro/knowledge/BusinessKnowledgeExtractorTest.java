package com.inquiro.knowledge;

import com.inquiro.business.BusinessProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessKnowledgeExtractorTest {

    private final BusinessKnowledgeExtractor extractor =
            new BusinessKnowledgeExtractor();

    @Test
    void shouldExtractBusinessProfileFromManualInformation() {

        BusinessProfile profile =
                extractor.extract(
                        KnowledgeDocument.manualForm(
                                """
                                        Business name:
                                        ABC Auto Care

                                        Business type:
                                        Vehicle Service Center

                                        Services:
                                        - Oil change
                                        - Brake repair

                                        Booking:
                                        Required

                                        Phone:
                                        071xxxxxxx

                                        Not supported:
                                        - Body painting
                                        """
                        )
                );

        assertEquals(
                "ABC Auto Care",
                profile.businessName()
        );

        assertEquals(
                "VEHICLE_SERVICE_CENTER",
                profile.businessType()
        );

        assertEquals(
                2,
                profile.services().size()
        );

        assertTrue(
                profile.services()
                        .stream()
                        .anyMatch(service ->
                                service.requestType()
                                        .equals("OIL_CHANGE"))
        );

        assertTrue(
                profile.knowledge()
                        .boundaries()
                        .notSupported()
                        .contains("Body painting")
        );
    }

    @Test
    void shouldExtractServiceRequirementsFromTabularInformation() {

        BusinessProfile profile =
                extractor.extract(
                        new KnowledgeDocument(
                                KnowledgeSource.EXCEL,
                                """
                                        Service\tAvailable\tRequirements
                                        Oil Change\tYes\tvehicleType, vehicleModel, preferredDate
                                        Body Painting\tNo\t
                                        """,
                                java.util.Map.of()
                        )
                );

        assertEquals(
                "OIL_CHANGE",
                profile.services()
                        .get(0)
                        .requestType()
        );

        assertEquals(
                java.util.List.of(
                        "vehicleType",
                        "vehicleModel",
                        "preferredDate"
                ),
                profile.services()
                        .get(0)
                        .requiredSlots()
        );

        assertTrue(
                profile.knowledge()
                        .boundaries()
                        .notSupported()
                        .contains("Body Painting")
        );
    }
}
