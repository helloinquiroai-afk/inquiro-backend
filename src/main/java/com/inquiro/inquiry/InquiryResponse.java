package com.inquiro.inquiry;


import java.util.List;

public record InquiryResponse(

        InquiryResult inquiry,

        List<String> missingFields,

        InquiryStatus status,

        String reply

) {
}
