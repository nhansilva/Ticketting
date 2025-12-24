package com.ticketing.common.dto.enums;

/**
 * Payment status enumeration
 */
public enum PaymentStatus {
    PENDING,      // Payment initiated
    PROCESSING,   // Payment being processed
    SUCCESS,      // Payment successful
    FAILED,       // Payment failed
    REFUNDED      // Payment refunded
}

