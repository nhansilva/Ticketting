package com.ticketing.common.interfaces;

/**
 * Marker interface — mọi entity có ID đều implement.
 * Cho phép generic operations không cần biết type cụ thể.
 *
 * @param <ID> kiểu ID (UUID, Long, String...)
 */
public interface Identifiable<ID> {
    ID getId();
}
