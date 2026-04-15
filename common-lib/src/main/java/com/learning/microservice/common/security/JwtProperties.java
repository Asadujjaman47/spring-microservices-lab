package com.learning.microservice.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binding for {@code common.jwt.*} — the shared HMAC secret used by every service. */
@ConfigurationProperties(prefix = "common.jwt")
public record JwtProperties(boolean enabled, String secret, String headerName, String tokenPrefix) {

  public JwtProperties {
    if (headerName == null || headerName.isBlank()) {
      headerName = "Authorization";
    }
    if (tokenPrefix == null) {
      tokenPrefix = "Bearer ";
    }
  }
}
