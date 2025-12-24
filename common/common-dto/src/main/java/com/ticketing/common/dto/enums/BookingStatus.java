package com.ticketing.common.dto.enums;

/**
 * Booking status enumeration
 */
public enum BookingStatus {
    PENDING,      // Booking created, waiting for payment
    CONFIRMED,    // Payment successful, booking confirmed
    CANCELLED,    // Booking cancelled
    EXPIRED,      // Reservation expired
    FAILED        // Payment failed
}

