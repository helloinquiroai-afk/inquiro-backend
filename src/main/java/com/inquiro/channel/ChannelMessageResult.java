package com.inquiro.channel;

import com.inquiro.inquiry.InquiryResponse;

public record ChannelMessageResult(
        String businessId,
        String sessionId,
        InquiryResponse response
) {
}
