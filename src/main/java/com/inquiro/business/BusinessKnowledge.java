package com.inquiro.business;

import java.util.List;
import java.util.Map;

public record BusinessKnowledge(

        String businessDescription,

        List<String> services,

        List<String> products,

        Map<String, String> facts,

        List<String> faqs,

        List<String> policies,

        String instructions,

        Map<String, String> operatingHours,

        List<String> locations,

        Map<String, String> contactInformation,

        Map<String, String> bookingRules,

        List<String> capabilities,

        List<String> restrictions,

        BusinessBoundaries boundaries

) {

    public BusinessKnowledge(
            String businessDescription,
            List<String> services,
            List<String> products,
            Map<String, String> facts,
            List<String> faqs,
            List<String> policies,
            String instructions) {

        this(
                businessDescription,
                services,
                products,
                facts,
                faqs,
                policies,
                instructions,
                Map.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                new BusinessBoundaries(
                        services,
                        List.of(),
                        List.of()
                )
        );
    }

    public BusinessKnowledge {

        services =
                services == null
                        ? List.of()
                        : List.copyOf(services);

        products =
                products == null
                        ? List.of()
                        : List.copyOf(products);

        facts =
                facts == null
                        ? Map.of()
                        : Map.copyOf(facts);

        faqs =
                faqs == null
                        ? List.of()
                        : List.copyOf(faqs);

        policies =
                policies == null
                        ? List.of()
                        : List.copyOf(policies);

        operatingHours =
                operatingHours == null
                        ? Map.of()
                        : Map.copyOf(operatingHours);

        locations =
                locations == null
                        ? List.of()
                        : List.copyOf(locations);

        contactInformation =
                contactInformation == null
                        ? Map.of()
                        : Map.copyOf(contactInformation);

        bookingRules =
                bookingRules == null
                        ? Map.of()
                        : Map.copyOf(bookingRules);

        capabilities =
                capabilities == null
                        ? List.of()
                        : List.copyOf(capabilities);

        restrictions =
                restrictions == null
                        ? List.of()
                        : List.copyOf(restrictions);

        boundaries =
                boundaries == null
                        ? new BusinessBoundaries(
                        services,
                        List.of(),
                        List.of()
                )
                        : boundaries;
    }
}
