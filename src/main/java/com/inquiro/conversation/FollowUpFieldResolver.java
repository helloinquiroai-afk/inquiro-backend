package com.inquiro.conversation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class FollowUpFieldResolver {

    private static final Pattern PHONE = Pattern.compile("^[+]?\\d[\\d ()-]{6,24}$");
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,99}$");

    private FollowUpFieldResolver() {
    }

    static Map<String, Object> resolve(
            Map<String, Object> extracted,
            String customerReply,
            List<String> missingFields) {

        Map<String, Object> resolved = new LinkedHashMap<>();
        if (extracted != null) {
            resolved.putAll(extracted);
        }

        if (customerReply == null || customerReply.isBlank()
                || missingFields == null || missingFields.isEmpty()) {
            return Map.copyOf(resolved);
        }

        String reply = customerReply.trim();
        String firstMissing = missingFields.get(0);

        if (!resolved.containsKey(firstMissing) || isBlank(resolved.get(firstMissing))) {
            Object value = inferDirectValue(firstMissing, reply);
            if (value != null) {
                resolved.put(firstMissing, value);
            }
        }

        return Map.copyOf(resolved);
    }

    private static Object inferDirectValue(String field, String reply) {
        if ("customerName".equalsIgnoreCase(field) && isSimpleName(reply)) {
            return reply;
        }
        if ("customerPhone".equalsIgnoreCase(field) && PHONE.matcher(reply).matches()) {
            return reply;
        }
        return null;
    }

    private static boolean isSimpleName(String value) {
        if (!NAME.matcher(value).matches()) return false;
        String normalized = value.toLowerCase();
        return !normalized.matches(".*\\b(already|provided|given|thanks|thank|yes|no|okay|ok|sure)\\b.*");
    }

    private static boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }
}
