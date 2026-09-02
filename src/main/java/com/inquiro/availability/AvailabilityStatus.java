package com.inquiro.availability;

public enum AvailabilityStatus {

    /**
     * Availability has been checked against a reliable,
     * sufficiently fresh source and can be confirmed to the customer.
     */
    CONFIRMED,

    /**
     * Business information indicates that the requested item/service
     * may be available, but the information is not sufficiently fresh
     * or reliable to confirm it.
     */
    INDICATED,

    /**
     * A reliable source says the requested item/service is not available.
     */
    UNAVAILABLE,

    /**
     * Availability cannot currently be determined from the information
     * available to Inquiro.
     */
    UNKNOWN
}
