package com.learning.microservice.user.web;

import com.learning.microservice.user.domain.User;
import com.learning.microservice.user.web.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

  UserResponse toResponse(User user);
}
