package com.learning.microservice.user.web;

import com.learning.microservice.common.api.ApiResponse;
import com.learning.microservice.user.service.AuthService;
import com.learning.microservice.user.web.dto.LoginRequest;
import com.learning.microservice.user.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
    return ApiResponse.ok(authService.login(req), "Login successful");
  }
}
