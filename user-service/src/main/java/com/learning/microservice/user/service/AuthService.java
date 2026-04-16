package com.learning.microservice.user.service;

import com.learning.microservice.common.error.BusinessException;
import com.learning.microservice.common.error.ErrorCode;
import com.learning.microservice.user.domain.User;
import com.learning.microservice.user.domain.UserRepository;
import com.learning.microservice.user.web.dto.LoginRequest;
import com.learning.microservice.user.web.dto.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final JwtIssuer jwtIssuer;

  public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtIssuer jwtIssuer) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.jwtIssuer = jwtIssuer;
  }

  public LoginResponse login(LoginRequest req) {
    User user = users.findByEmail(req.email()).orElseThrow(this::invalid);
    if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
      throw invalid();
    }
    JwtIssuer.IssuedToken issued = jwtIssuer.issue(user.getId(), user.getEmail());
    return new LoginResponse(issued.token(), "Bearer", issued.expiresAt());
  }

  private BusinessException invalid() {
    return new BusinessException(ErrorCode.USER_INVALID_CREDENTIALS, "Invalid credentials");
  }
}
