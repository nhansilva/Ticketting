package com.ticketing.user.mapper;

import com.ticketing.user.dto.response.UserPreferencesResponse;
import com.ticketing.user.dto.response.UserResponse;
import com.ticketing.user.entity.User;
import com.ticketing.user.entity.UserPreferences;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * User mapper for entity to DTO conversion
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "id", target = "userId")
    @Mapping(target = "status", expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    UserResponse toResponse(User user);

    @Mapping(source = "userId", target = "userId")
    UserPreferencesResponse toPreferencesResponse(UserPreferences preferences);
}

