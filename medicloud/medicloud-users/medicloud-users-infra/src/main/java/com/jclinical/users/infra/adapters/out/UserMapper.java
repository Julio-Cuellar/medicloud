package com.jclinical.users.infra.adapters.out;

import com.jclinical.users.domain.model.User;
import com.jclinical.users.infra.adapters.in.web.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserEntity toEntity(User domain);

    User toDomain(UserEntity entity);

    UserResponse toResponse(User domain);
}
