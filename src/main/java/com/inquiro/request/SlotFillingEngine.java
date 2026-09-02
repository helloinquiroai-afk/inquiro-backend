package com.inquiro.request;

import com.inquiro.ai.RequestAnalysis;
import com.inquiro.business.BusinessProfile;
import com.inquiro.business.BusinessProfileProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotFillingEngine {

    private final BusinessProfileProvider businessProfileProvider;

    public List<String> findMissingSlots(
            RequestAnalysis analysis) {

        BusinessProfile businessProfile =
                businessProfileProvider.get();

        return findMissingSlots(
                analysis,
                businessProfile
        );
    }

    public List<String> findMissingSlots(
            RequestAnalysis analysis,
            BusinessProfile businessProfile) {

        RequestDefinition definition =
                businessProfile.services()
                        .stream()
                        .filter(service ->
                                service.requestType()
                                        .equalsIgnoreCase(
                                                analysis.intent()
                                        ))
                        .findFirst()
                        .orElse(null);

        if (definition == null) {
            return List.of();
        }

        return definition.requiredSlots()
                .stream()
                .filter(slot ->
                        isMissing(
                                analysis,
                                slot
                        ))
                .toList();
    }

    private boolean isMissing(
            RequestAnalysis analysis,
            String slot) {

        if (!analysis.entities().containsKey(slot)) {
            return true;
        }

        Object value =
                analysis.entities()
                        .get(slot);

        return value == null ||
                String.valueOf(value).isBlank();
    }
}
