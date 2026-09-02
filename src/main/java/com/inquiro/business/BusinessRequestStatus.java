package com.inquiro.business;

public enum BusinessRequestStatus {

    /**
     * Request has been received but the business
     * has not confirmed it yet.
     */
    PENDING_CONFIRMATION,

    /**
     * Business has confirmed the request.
     */
    CONFIRMED,

    /**
     * Business rejected the request.
     */
    REJECTED,

    /**
     * Request was cancelled.
     */
    CANCELLED
}
