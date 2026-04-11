package com.ticketing.common.interfaces.service;

import com.ticketing.common.dto.response.PageResponse;
import reactor.core.publisher.Mono;

/**
 * Base service contract — Template Method pattern.
 * Định nghĩa contract chuẩn cho CRUD operations trên mọi service.
 *
 * Service implementation:
 * <pre>
 * {@code
 * @Service
 * public class EventServiceImpl implements CrudService<UUID, CreateEventRequest, EventResponse> {
 *     // implement các method bắt buộc
 * }
 * }
 * </pre>
 *
 * @param <ID>  Kiểu ID của entity
 * @param <REQ> Request DTO (create/update dùng chung hoặc tách riêng)
 * @param <RES> Response DTO
 */
public interface CrudService<ID, REQ, RES> {

    Mono<RES> findById(ID id);

    Mono<PageResponse<RES>> findAll(com.ticketing.common.dto.request.PageRequest pageRequest);

    Mono<RES> create(REQ request);

    Mono<RES> update(ID id, REQ request);

    Mono<Void> delete(ID id);
}
