package com.inquiro.inquiry;

import java.util.List;

public record InquiryResponse(
        InquiryResult inquiry,
        List<String> missingFields,
        InquiryStatus status,
        String reply,
        @com.fasterxml.jackson.annotation.JsonIgnore String knowledgeReply,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        String bookingId
) {
    public InquiryResponse(InquiryResult inquiry, List<String> missingFields, InquiryStatus status, String reply) {
        this(inquiry, missingFields, status, reply, "", null);
    }

    public InquiryResponse(InquiryResult inquiry, List<String> missingFields, InquiryStatus status, String reply,
                           String bookingId) {
        this(inquiry, missingFields, status, reply, "", bookingId);
    }

    public InquiryResponse(InquiryResult inquiry, List<String> missingFields, InquiryStatus status, String reply,
                           String knowledgeReply, String bookingId) {
        this.inquiry = inquiry;
        this.missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
        this.status = status;
        this.reply = reply;
        this.knowledgeReply = knowledgeReply == null ? "" : knowledgeReply;
        this.bookingId = bookingId;
    }

    public InquiryResponse withKnowledgeReply(String answer) {
        if (answer == null || answer.isBlank()) return this;
        return new InquiryResponse(inquiry, missingFields, status, answer + " " + reply, answer, bookingId);
    }
}
