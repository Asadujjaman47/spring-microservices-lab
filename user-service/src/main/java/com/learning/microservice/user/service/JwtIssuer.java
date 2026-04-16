package com.learning.microservice.user.service;

import com.learning.microservice.common.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Signs HMAC JWTs using the shared {@code common.jwt.secret}. */
@Component
public class JwtIssuer {

  private final SecretKey key;
  private final Duration ttl;

  public JwtIssuer(JwtProperties jwtProps, AuthProperties authProps) {
    this.key = Keys.hmacShaKeyFor(jwtProps.secret().getBytes(StandardCharsets.UTF_8));
    this.ttl = Duration.ofMinutes(authProps.tokenTtlMinutes());
  }

  public IssuedToken issue(UUID userId, String email) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(ttl);
    String token =
        Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact();
    return new IssuedToken(token, expiresAt);
  }

  public record IssuedToken(String token, Instant expiresAt) {}
}
