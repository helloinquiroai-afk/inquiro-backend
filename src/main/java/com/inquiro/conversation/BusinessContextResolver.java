package com.inquiro.conversation;

import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestDefinition;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BusinessContextResolver {

    /**
     * Resolves only information that is unambiguously supplied by the
     * configured business context. Customer-specific information is never
     * guessed from business context.
     *
     * The main example is a service requiring a location when the business
     * has exactly one configured location. Multiple configured locations
     * remain a customer choice and therefore stay missing.
     */
    public Map<String, Object> resolve(
            RequestDefinition definition,
            Map<String, Object> currentFields,
            BusinessProfile profile) {

        Map<String, Object> resolved = new LinkedHashMap<>();
        if (currentFields != null) {
            resolved.putAll(currentFields);
        }
        if (definition == null || profile == null || profile.knowledge() == null) {
            return Map.copyOf(resolved);
        }

        for (String slot : definition.requiredSlots()) {
            if (!isEmpty(resolved.get(slot))) {
                continue;
            }

            if (isLocationLike(slot)) {
                List<String> locations = profile.knowledge().locations();
                if (locations != null && locations.size() == 1 && !isEmpty(locations.get(0))) {
                    resolved.put(slot, locations.get(0));
                }
            }
        }

        return Map.copyOf(resolved);
    }

    public boolean isLocationLike(String slot) {
        if (slot == null) return false;
        String normalized = slot.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return normalized.equals("location")
                || normalized.equals("branch")
                || normalized.equals("property")
                || normalized.equals("site")
                || normalized.equals("cliniclocation")
                || normalized.equals("storelocation")
                || normalized.equals("hotel");
    }

    private boolean isEmpty(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }
}
