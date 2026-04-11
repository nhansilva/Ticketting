package com.ticketing.common.dto.enums;

/**
 * Booking lifecycle states.
 *
 * Flow:
 *   PENDING ──(payment success)──► CONFIRMED
 *   PENDING ──(timeout / cancel)──► CANCELLED
 *   CONFIRMED ──(admin refund)───► REFUNDED
 */
public enum BookingStatus {
    /** Booking tạo, chờ thanh toán — hết 10 phút tự hủy */
    PENDING,
    /** Thanh toán thành công */
    CONFIRMED,
    /** Đã hủy (hết thời gian hoặc user hủy) */
    CANCELLED,
    /** Timeout mà không thanh toán */
    EXPIRED,
    /** Lỗi trong quá trình tạo */
    FAILED,
    /** Admin hoàn tiền */
    REFUNDED
}
