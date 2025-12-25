package com.ticketing.user.mapper;

import com.ticketing.user.dto.response.UserPreferencesResponse;
import com.ticketing.user.dto.response.UserResponse;
import com.ticketing.user.entity.User;
import com.ticketing.user.entity.UserPreferences;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * User mapper for entity to DTO conversion
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(source = "id", target = "userId")
    @Mapping(source = "status", target = "status", expression = "java(user.getStatus().name())")
    UserResponse toResponse(User user);

    @Mapping(source = "userId", target = "userId")
    UserPreferencesResponse toPreferencesResponse(UserPreferences preferences);
}

