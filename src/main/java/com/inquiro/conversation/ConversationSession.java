package com.inquiro.conversation;

import com.inquiro.inquiry.InquiryResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationSession {

    private String sessionId;

    private InquiryResult inquiry;

    private List<String> missingFields;

    private Instant lastUpdated;

}
