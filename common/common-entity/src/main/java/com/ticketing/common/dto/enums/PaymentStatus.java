package com.ticketing.common.dto.enums;

/**
 * Payment transaction states.
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    /** Thanh toán thành công */
    SUCCESS,
    FAILED,
    /** Hoàn tiền (chỉ ADMIN trigger) */
    REFUNDED
}
