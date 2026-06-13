package com.inquiro.conversation;

import com.inquiro.inquiry.InquiryResult;

import java.time.Instant;
import java.util.List;

public class ConversationSession {

    private String sessionId;

    private InquiryResult inquiry;

    private List<String> missingFields;

    private Instant lastUpdated;

}
