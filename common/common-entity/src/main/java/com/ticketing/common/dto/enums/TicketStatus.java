package com.ticketing.common.dto.enums;

/**
 * Seat/ticket availability states.
 */
public enum TicketStatus {
    AVAILABLE,
    /** Đang bị lock bởi một booking PENDING (TTL 10 phút) */
    RESERVED,
    /** Đã bán (booking CONFIRMED) */
    SOLD,
    CANCELLED,
    /** Lock hết hạn, trở về AVAILABLE */
    EXPIRED
}
