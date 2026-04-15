package com.learning.microservice.user.web;

import com.learning.microservice.common.api.ApiResponse;
import com.learning.microservice.user.service.UserService;
import com.learning.microservice.user.web.dto.CreateUserRequest;
import com.learning.microservice.user.web.dto.UpdateUserRequest;
import com.learning.microservice.user.web.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
    return ApiResponse.ok(userService.create(req), "User created");
  }

  @GetMapping
  public ApiResponse<List<UserResponse>> list() {
    return ApiResponse.ok(userService.list());
  }

  @GetMapping("/{id}")
  public ApiResponse<UserResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(userService.getById(id));
  }

  @PutMapping("/{id}")
  public ApiResponse<UserResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
    return ApiResponse.ok(userService.update(id, req), "User updated");
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    userService.delete(id);
  }
}
