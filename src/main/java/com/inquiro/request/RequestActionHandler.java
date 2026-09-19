package com.inquiro.request;

import com.inquiro.business.BusinessAccount;
import com.inquiro.inquiry.InquiryResponse;
import com.inquiro.inquiry.InquiryResult;

public interface RequestActionHandler {

    RequestActionType actionType();

    InquiryResponse handle(
            BusinessAccount businessAccount,
            String sessionId,
            InquiryResult inquiry
    );
}
