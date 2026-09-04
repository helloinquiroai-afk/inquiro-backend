package com.inquiro.business;

public enum BusinessRequestStatus {

    /**
     * Request has been received but the business
     * has not confirmed it yet.
     */
    PENDING_CONFIRMATION,

    /**
     * Request requires a member of the business
     * to review it manually.
     */
    PENDING_REVIEW,

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