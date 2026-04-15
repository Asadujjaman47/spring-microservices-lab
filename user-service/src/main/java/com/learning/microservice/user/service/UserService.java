package com.learning.microservice.user.service;

import com.learning.microservice.common.error.BusinessException;
import com.learning.microservice.common.error.ErrorCode;
import com.learning.microservice.user.domain.User;
import com.learning.microservice.user.domain.UserRepository;
import com.learning.microservice.user.web.UserMapper;
import com.learning.microservice.user.web.dto.CreateUserRequest;
import com.learning.microservice.user.web.dto.UpdateUserRequest;
import com.learning.microservice.user.web.dto.UserResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

  private final UserRepository users;
  private final UserMapper mapper;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository users, UserMapper mapper, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.mapper = mapper;
    this.passwordEncoder = passwordEncoder;
  }

  public UserResponse create(CreateUserRequest req) {
    if (users.existsByEmail(req.email())) {
      throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Email already registered");
    }
    User user = new User();
    user.setEmail(req.email());
    user.setPasswordHash(passwordEncoder.encode(req.password()));
    user.setFirstName(req.firstName());
    user.setLastName(req.lastName());
    return mapper.toResponse(users.save(user));
  }

  @Transactional(readOnly = true)
  public UserResponse getById(UUID id) {
    return users.findById(id).map(mapper::toResponse).orElseThrow(this::notFound);
  }

  @Transactional(readOnly = true)
  public List<UserResponse> list() {
    return users.findAll().stream().map(mapper::toResponse).toList();
  }

  public UserResponse update(UUID id, UpdateUserRequest req) {
    User user = users.findById(id).orElseThrow(this::notFound);
    user.setFirstName(req.firstName());
    user.setLastName(req.lastName());
    return mapper.toResponse(user);
  }

  public void delete(UUID id) {
    if (!users.existsById(id)) {
      throw notFound();
    }
    users.deleteById(id);
  }

  private BusinessException notFound() {
    return new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found");
  }
}
