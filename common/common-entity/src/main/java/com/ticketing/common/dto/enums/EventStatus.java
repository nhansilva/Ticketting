package com.ticketing.common.dto.enums;

/**
 * Event lifecycle states.
 */
public enum EventStatus {
    /** Admin đang tạo, chưa publish */
    DRAFT,
    /** Đã publish, hiển thị public */
    PUBLISHED,
    /** Đang mở bán vé */
    ON_SALE,
    /** Hết vé */
    SOLD_OUT,
    /** Admin hủy sự kiện */
    CANCELLED,
    /** Sự kiện đã diễn ra xong */
    COMPLETED
}
