package com.ticketing.common.dto.enums;

/**
 * Event status enumeration
 */
public enum EventStatus {
    DRAFT,        // Event is being created
    PUBLISHED,    // Event is published and tickets available
    ON_SALE,      // Tickets are on sale
    SOLD_OUT,     // All tickets sold
    CANCELLED,    // Event cancelled
    COMPLETED     // Event completed
}

