package com.inquiro.inquiry;

import com.inquiro.domain.ServiceDefinition;
import com.inquiro.domain.ServiceDefinitionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryProcessor {

    private final ServiceDefinitionRegistry registry;

    public List<String> findMissingFields(
            InquiryResult inquiry) {

        ServiceDefinition definition =
                registry.get(inquiry.service());

        if (definition == null) {
            return List.of();
        }

        return definition.requiredFields()
                .stream()
                .filter(field -> {

                    Object value =
                            inquiry.fields().get(field);

                    return value == null
                            || value.toString().isBlank()
                            || "UNKNOWN".equalsIgnoreCase(
                            value.toString());

                })
                .toList();
    }
}