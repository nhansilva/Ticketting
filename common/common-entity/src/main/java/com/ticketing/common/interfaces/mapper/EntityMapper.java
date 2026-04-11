package com.ticketing.common.interfaces.mapper;

import java.util.List;

/**
 * Base mapper interface — Strategy/Adapter pattern.
 * MapStruct implement tự động khi service @Mapper extends interface này.
 *
 * Cách dùng:
 * <pre>
 * {@code
 * @Mapper(componentModel = "spring")
 * public interface UserMapper extends EntityMapper<User, UserResponse> {
 *     // Override nếu cần mapping phức tạp
 *     @Mapping(target = "fullName", expression = "java(e.getFirstName() + ' ' + e.getLastName())")
 *     UserResponse toDto(User entity);
 * }
 * }
 * </pre>
 *
 * @param <E> Entity type
 * @param <D> DTO type
 */
public interface EntityMapper<E, D> {

    D toDto(E entity);

    E toEntity(D dto);

    default List<D> toDtoList(List<E> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(this::toDto).toList();
    }

    default List<E> toEntityList(List<D> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(this::toEntity).toList();
    }
}
