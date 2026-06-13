package com.inquiro.inquiry;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class InquiryNormalizer {

    public InquiryResult normalize(InquiryResult inquiry) {

        Map<String, Object> fields =
                new HashMap<>(inquiry.fields());

        Object checkOutDate =
                fields.get("checkOutDate");

        if (checkOutDate != null
                && !fields.containsKey("durationNights")) {

            String value = checkOutDate.toString();

            Matcher matcher =
                    Pattern.compile("(\\d+)").matcher(value);

            if (matcher.find()) {
                fields.put(
                        "durationNights",
                        Integer.parseInt(
                                matcher.group(1)
                        )
                );
            }
        }

        return new InquiryResult(
                inquiry.domain(),
                inquiry.service(),
                fields
        );
    }
}
