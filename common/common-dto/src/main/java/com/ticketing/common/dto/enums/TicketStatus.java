package com.ticketing.common.dto.enums;

/**
 * Ticket status enumeration
 */
public enum TicketStatus {
    AVAILABLE,    // Ticket is available for booking
    RESERVED,     // Ticket is reserved (waiting for payment)
    SOLD,         // Ticket is sold (payment confirmed)
    CANCELLED,    // Ticket booking was cancelled
    EXPIRED       // Reservation expired
}

