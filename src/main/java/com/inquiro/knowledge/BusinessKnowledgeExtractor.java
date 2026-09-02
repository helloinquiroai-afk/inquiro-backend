package com.inquiro.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.business.BusinessBoundaries;
import com.inquiro.business.BusinessKnowledge;
import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class BusinessKnowledgeExtractor {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public BusinessProfile extract(
            KnowledgeDocument document) {

        ParsedKnowledge parsed =
                document.source() == KnowledgeSource.JSON
                        ? parseJson(document.content())
                        : parseText(document.content());

        return buildProfile(parsed);
    }

    private BusinessProfile buildProfile(
            ParsedKnowledge parsed) {

        String businessName =
                firstText(
                        parsed,
                        parsed.values(),
                        "businessname",
                        "name",
                        "business"
                );

        if (businessName.isBlank()) {
            businessName = "Unnamed Business";
        }

        String businessType =
                firstText(
                        parsed,
                        parsed.values(),
                        "businesstype",
                        "type",
                        "category"
                );

        if (businessType.isBlank()) {
            businessType = "GENERAL_BUSINESS";
        }

        String description =
                firstText(
                        parsed,
                        parsed.values(),
                        "description",
                        "businessdescription",
                        "about"
                );

        List<String> services =
                firstList(
                        parsed.sections(),
                        "services",
                        "supportedservices"
                );

        if (services.isEmpty()) {
            services = parsed.serviceRows()
                    .stream()
                    .map(ServiceRow::name)
                    .toList();
        }

        List<String> products =
                firstList(
                        parsed.sections(),
                        "products",
                        "vehicles"
                );

        Map<String, String> facts =
                new LinkedHashMap<>();
        facts.putAll(parsed.values());

        Map<String, String> operatingHours =
                singleValueMap(
                        "default",
                        firstText(
                                parsed,
                                parsed.values(),
                                "openinghours",
                                "hours",
                                "businesshours"
                        )
                );

        List<String> policies =
                new ArrayList<>(
                        firstList(
                                parsed.sections(),
                                "policies",
                                "policy"
                        )
                );

        String cancellation =
                firstText(
                        parsed,
                        parsed.values(),
                        "cancellation",
                        "cancellationpolicy"
                );

        if (!cancellation.isBlank()) {
            policies.add("Cancellation: " + cancellation);
        }

        Map<String, String> contact =
                contactInformation(parsed);

        Map<String, String> bookingRules =
                bookingRules(parsed);

        List<String> notSupported =
                firstList(
                        parsed.sections(),
                        "notsupported",
                        "unsupported",
                        "restrictions"
                );

        List<String> requiresHuman =
                firstList(
                        parsed.sections(),
                        "requireshuman",
                        "manualreview",
                        "humanrequired"
                );

        for (ServiceRow row : parsed.serviceRows()) {

            if (!row.available()) {
                notSupported =
                        appendDistinct(
                                notSupported,
                                row.name()
                        );
            }
        }

        BusinessBoundaries boundaries =
                new BusinessBoundaries(
                        services,
                        notSupported,
                        requiresHuman
                );

        BusinessKnowledge knowledge =
                new BusinessKnowledge(
                        description,
                        services,
                        products,
                        facts,
                        firstList(
                                parsed.sections(),
                                "faqs",
                                "faq"
                        ),
                        policies,
                        defaultInstructions(),
                        operatingHours,
                        firstList(
                                parsed.sections(),
                                "locations",
                                "location"
                        ),
                        contact,
                        bookingRules,
                        services,
                        notSupported,
                        boundaries
                );

        List<RequestDefinition> definitions =
                serviceDefinitions(
                        services,
                        parsed.serviceRows(),
                        bookingRules
                );

        return new BusinessProfile(
                businessName,
                serviceCode(businessType),
                description,
                definitions,
                knowledge
        );
    }

    private ParsedKnowledge parseJson(
            String content) {

        try {

            Map<String, Object> root =
                    objectMapper.readValue(
                            content,
                            new TypeReference<>() {
                            }
                    );

            ParsedKnowledge parsed =
                    new ParsedKnowledge();

            flattenJson(
                    "",
                    root,
                    parsed
            );

            return parsed;

        } catch (Exception e) {

            return parseText(content);
        }
    }

    @SuppressWarnings("unchecked")
    private void flattenJson(
            String prefix,
            Map<String, Object> values,
            ParsedKnowledge parsed) {

        for (Map.Entry<String, Object> entry : values.entrySet()) {

            String key =
                    normalizeKey(
                            prefix + entry.getKey()
                    );

            Object value =
                    entry.getValue();

            if (value instanceof List<?> list) {

                parsed.sections()
                        .put(
                                key,
                                list.stream()
                                        .map(String::valueOf)
                                        .toList()
                        );

            } else if (value instanceof Map<?, ?> map) {

                flattenJson(
                        key,
                        (Map<String, Object>) map,
                        parsed
                );

            } else if (value != null) {

                parsed.values()
                        .put(
                                key,
                                String.valueOf(value)
                        );
            }
        }
    }

    private ParsedKnowledge parseText(
            String content) {

        ParsedKnowledge parsed =
                new ParsedKnowledge();

        String currentSection =
                null;

        for (String rawLine : content.split("\\R")) {

            String line =
                    rawLine.strip();

            if (line.isBlank()) {
                continue;
            }

            if (looksLikeTableHeader(line)) {
                parseServiceTable(
                        content,
                        parsed
                );
                continue;
            }

            if (line.endsWith(":")) {
                currentSection =
                        normalizeKey(
                                line.substring(
                                        0,
                                        line.length() - 1
                                )
                        );
                parsed.sections()
                        .putIfAbsent(
                                currentSection,
                                new ArrayList<>()
                        );
                continue;
            }

            if (line.startsWith("-") && currentSection != null) {

                parsed.sections()
                        .computeIfAbsent(
                                currentSection,
                                ignored -> new ArrayList<>()
                        )
                        .add(
                                line.substring(1)
                                        .strip()
                        );
                continue;
            }

            int colon =
                    line.indexOf(':');

            if (colon > 0) {

                String key =
                        normalizeKey(
                                line.substring(0, colon)
                        );

                String value =
                        line.substring(colon + 1)
                                .strip();

                parsed.values()
                        .put(
                                key,
                                value
                        );

                currentSection = key;
                continue;
            }

            if (currentSection != null) {

                parsed.sections()
                        .computeIfAbsent(
                                currentSection,
                                ignored -> new ArrayList<>()
                        )
                        .add(line);
            }
        }

        return parsed;
    }

    private void parseServiceTable(
            String content,
            ParsedKnowledge parsed) {

        for (String rawLine : content.split("\\R")) {

            String line =
                    rawLine.strip();

            if (line.isBlank() ||
                    looksLikeTableHeader(line)) {
                continue;
            }

            String[] columns =
                    splitColumns(line);

            if (columns.length < 2) {
                continue;
            }

            String service =
                    columns[0].strip();

            boolean available =
                    columns[1].strip()
                            .equalsIgnoreCase("yes")
                            ||
                            columns[1].strip()
                                    .equalsIgnoreCase("available")
                            ||
                            columns[1].strip()
                                    .equalsIgnoreCase("true");

            List<String> requirements =
                    columns.length > 2
                            ? splitList(columns[2])
                            : List.of();

            parsed.serviceRows()
                    .add(
                            new ServiceRow(
                                    service,
                                    available,
                                    requirements
                            )
                    );
        }
    }

    private List<RequestDefinition> serviceDefinitions(
            List<String> services,
            List<ServiceRow> serviceRows,
            Map<String, String> bookingRules) {

        Map<String, ServiceRow> rowsByService =
                new LinkedHashMap<>();

        for (ServiceRow row : serviceRows) {
            rowsByService.put(
                    serviceCode(row.name()),
                    row
            );
        }

        List<RequestDefinition> definitions =
                new ArrayList<>();

        boolean bookingRequired =
                bookingRules.values()
                        .stream()
                        .anyMatch(value ->
                                value.toLowerCase(Locale.ROOT)
                                        .contains("required"));

        for (String service : services) {

            ServiceRow row =
                    rowsByService.get(
                            serviceCode(service)
                    );

            List<String> requiredSlots =
                    row == null
                            ? List.of()
                            : row.requirements();

            if (requiredSlots.isEmpty() &&
                    bookingRequired) {

                requiredSlots =
                        List.of(
                                "preferredDate",
                                "preferredTime",
                                "customerName",
                                "customerPhone"
                        );
            }

            definitions.add(
                    new RequestDefinition(
                            serviceCode(service),
                            service,
                            requiredSlots,
                            promptsFor(requiredSlots)
                    )
            );
        }

        return definitions;
    }

    private Map<String, String> promptsFor(
            List<String> slots) {

        Map<String, String> prompts =
                new LinkedHashMap<>();

        for (String slot : slots) {
            prompts.put(
                    slot,
                    "Could you please provide "
                            + humanize(slot)
                            + "?"
            );
        }

        return prompts;
    }

    private Map<String, String> bookingRules(
            ParsedKnowledge parsed) {

        Map<String, String> rules =
                new LinkedHashMap<>();

        putIfPresent(
                rules,
                "booking",
                firstText(
                        parsed,
                        parsed.values(),
                        "booking",
                        "bookingrequired"
                )
        );

        putIfPresent(
                rules,
                "cancellation",
                firstText(
                        parsed,
                        parsed.values(),
                        "cancellation",
                        "cancellationpolicy"
                )
        );

        return rules;
    }

    private Map<String, String> contactInformation(
            ParsedKnowledge parsed) {

        Map<String, String> contact =
                new LinkedHashMap<>();

        putIfPresent(
                contact,
                "phone",
                firstText(
                        parsed,
                        parsed.values(),
                        "phone",
                        "contactphone",
                        "telephone"
                )
        );

        putIfPresent(
                contact,
                "email",
                firstText(
                        parsed,
                        parsed.values(),
                        "email",
                        "contactemail"
                )
        );

        putIfPresent(
                contact,
                "website",
                firstText(
                        parsed,
                        parsed.values(),
                        "website",
                        "url"
                )
        );

        return contact;
    }

    private String defaultInstructions() {

        return """
                Answer customers using only the business information provided.
                Never invent prices, availability, services, products, rules, or policies.
                If information is unavailable, say that the business has not provided it.
                If contact information is available, provide it exactly as given.
                """;
    }

    private Map<String, String> singleValueMap(
            String key,
            String value) {

        if (value == null || value.isBlank()) {
            return Map.of();
        }

        return Map.of(key, value);
    }

    private void putIfPresent(
            Map<String, String> target,
            String key,
            String value) {

        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private String firstValue(
            Map<String, String> values,
            String... keys) {

        for (String key : keys) {

            String value =
                    values.get(
                            normalizeKey(key)
                    );

            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private String firstText(
            ParsedKnowledge parsed,
            Map<String, String> values,
            String... keys) {

        String value =
                firstValue(
                        values,
                        keys
                );

        if (!value.isBlank()) {
            return value;
        }

        for (String key : keys) {

            List<String> section =
                    parsed.sections()
                            .get(
                                    normalizeKey(key)
                            );

            if (section != null && !section.isEmpty()) {
                return String.join(
                        " ",
                        section
                );
            }
        }

        return "";
    }

    private List<String> firstList(
            Map<String, List<String>> sections,
            String... keys) {

        for (String key : keys) {

            List<String> values =
                    sections.get(
                            normalizeKey(key)
                    );

            if (values != null && !values.isEmpty()) {
                return values;
            }
        }

        return List.of();
    }

    private List<String> appendDistinct(
            List<String> values,
            String value) {

        Set<String> distinct =
                new LinkedHashSet<>(values);

        distinct.add(value);

        return List.copyOf(distinct);
    }

    private List<String> splitList(
            String value) {

        if (value == null || value.isBlank() ||
                "—".equals(value.strip()) ||
                "-".equals(value.strip())) {
            return List.of();
        }

        return List.of(
                value.split("\\s*,\\s*")
        );
    }

    private String[] splitColumns(
            String line) {

        if (line.contains("\t")) {
            return line.split("\\t");
        }

        if (line.matches(".*\\s{2,}.*")) {
            return line.split("\\s{2,}");
        }

        return line.split(
                ",",
                3
        );
    }

    private boolean looksLikeTableHeader(
            String line) {

        String normalized =
                line.toLowerCase(Locale.ROOT);

        return normalized.contains("service")
                && normalized.contains("available")
                && normalized.contains("requirement");
    }

    private String serviceCode(
            String value) {

        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }

        return value.strip()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String normalizeKey(
            String value) {

        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }

    private String humanize(
            String field) {

        return field.replaceAll(
                        "([a-z])([A-Z])",
                        "$1 $2"
                )
                .toLowerCase(Locale.ROOT);
    }

    private record ServiceRow(
            String name,
            boolean available,
            List<String> requirements
    ) {
    }

    private static final class ParsedKnowledge {

        private final Map<String, String> values =
                new LinkedHashMap<>();

        private final Map<String, List<String>> sections =
                new LinkedHashMap<>();

        private final List<ServiceRow> serviceRows =
                new ArrayList<>();

        Map<String, String> values() {
            return values;
        }

        Map<String, List<String>> sections() {
            return sections;
        }

        List<ServiceRow> serviceRows() {
            return serviceRows;
        }
    }
}
