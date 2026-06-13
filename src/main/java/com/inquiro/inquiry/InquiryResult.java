package com.inquiro.inquiry;

import java.util.Map;

public record InquiryResult(
        String domain,
        String service,
        Map<String, Object> fields
) {
}
