package com.learning.microservice.user.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binding for {@code user.auth.*} — JWT issuance tunables. */
@ConfigurationProperties(prefix = "user.auth")
public record AuthProperties(long tokenTtlMinutes) {

  public AuthProperties {
    if (tokenTtlMinutes <= 0) {
      tokenTtlMinutes = 60;
    }
  }
}
