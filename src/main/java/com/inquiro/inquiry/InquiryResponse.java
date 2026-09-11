package com.inquiro.inquiry;


import java.util.List;

public record InquiryResponse(

        InquiryResult inquiry,

        List<String> missingFields,

        InquiryStatus status,

        String reply,

        @com.fasterxml.jackson.annotation.JsonIgnore String knowledgeReply

) {
    public InquiryResponse(InquiryResult inquiry, List<String> missingFields, InquiryStatus status, String reply) {
        this(inquiry, missingFields, status, reply, "");
    }

    public InquiryResponse withKnowledgeReply(String answer) {
        if (answer == null || answer.isBlank()) return this;
        return new InquiryResponse(inquiry, missingFields, status, answer + " " + reply, answer);
    }
}
