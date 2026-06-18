package com.inquiro.request;

import com.inquiro.ai.RequestAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotFillingEngine {

    private final RequestRegistry registry;

    public List<String> findMissingSlots(
            RequestAnalysis analysis) {

        RequestDefinition definition =
                registry.get(
                        analysis.requestType());

        if (definition == null) {
            return List.of();
        }

        return definition.requiredSlots()
                .stream()
                .filter(slot ->
                        !analysis.entities()
                                .containsKey(slot))
                .toList();
    }
}
