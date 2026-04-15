package com.learning.microservice.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// spring-security-crypto ships without Spring Security; Boot doesn't auto-wire a PasswordEncoder
// unless the full security starter is present. Full Spring Security arrives at the gateway in
// Phase 5 — until then we declare the bean ourselves.
@Configuration
public class PasswordEncoderConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
