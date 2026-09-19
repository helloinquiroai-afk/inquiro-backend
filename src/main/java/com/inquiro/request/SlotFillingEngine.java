package com.inquiro.request;

import com.inquiro.ai.RequestAnalysis;
import com.inquiro.business.BusinessProfile;
import com.inquiro.business.BusinessProfileProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotFillingEngine {

    private final BusinessProfileProvider businessProfileProvider;

    public List<String> findMissingSlots(RequestAnalysis analysis) {
        return findMissingSlots(analysis, businessProfileProvider.get());
    }

    public List<String> findMissingSlots(
            RequestAnalysis analysis,
            BusinessProfile businessProfile) {

        if (analysis == null || businessProfile == null || businessProfile.services() == null) {
            return List.of();
        }

        RequestDefinition definition = businessProfile.services()
                .stream()
                .filter(service -> service.requestType().equalsIgnoreCase(analysis.intent()))
                .findFirst()
                .orElse(null);

        if (definition == null) {
            return List.of();
        }

        List<String> missing = new ArrayList<>();
        addMissing(missing, analysis, definition.requiredSlots());

        if (definition.actionType() == RequestActionType.BOOKING) {
            addMissing(missing, analysis, definition.actionRequiredSlots());
        }

        return List.copyOf(missing);
    }

    private void addMissing(
            List<String> missing,
            RequestAnalysis analysis,
            List<String> slots) {
        if (slots == null) {
            return;
        }
        for (String slot : slots) {
            if (!missing.contains(slot) && isMissing(analysis, slot)) {
                missing.add(slot);
            }
        }
    }

    private boolean isMissing(RequestAnalysis analysis, String slot) {
        if (slot == null || slot.isBlank() || analysis.entities() == null) {
            return true;
        }
        if (!analysis.entities().containsKey(slot)) {
            return true;
        }
        Object value = analysis.entities().get(slot);
        return value == null || String.valueOf(value).isBlank();
    }
}
