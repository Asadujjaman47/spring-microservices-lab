package com.learning.microservice.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.microservice.user.web.dto.CreateUserRequest;
import com.learning.microservice.user.web.dto.LoginRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

class AuthApiTests extends AbstractIntegrationTest {

  @LocalServerPort int port;
  @Autowired RestClient.Builder clientBuilder;

  @Value("${common.jwt.secret}")
  String jwtSecret;

  @Test
  void loginReturnsSignedJwtWithUserIdSubject() {
    RestClient client = clientBuilder.baseUrl("http://localhost:" + port).build();
    client
        .post()
        .uri("/api/v1/users")
        .body(new CreateUserRequest("grace@example.com", "super-secret", "Grace", "Hopper"))
        .retrieve()
        .toBodilessEntity();

    ResponseEntity<Map<String, Object>> resp =
        client
            .post()
            .uri("/api/v1/auth/login")
            .body(new LoginRequest("grace@example.com", "super-secret"))
            .retrieve()
            .toEntity(new org.springframework.core.ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
    String token = (String) data.get("token");
    assertThat(token).isNotBlank();
    assertThat(data.get("tokenType")).isEqualTo("Bearer");

    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    assertThat(claims.getSubject()).isNotBlank();
    assertThat(claims.get("email")).isEqualTo("grace@example.com");
    assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
  }

  @Test
  void loginWithWrongPasswordIs401() {
    RestClient client = clientBuilder.baseUrl("http://localhost:" + port).build();
    client
        .post()
        .uri("/api/v1/users")
        .body(new CreateUserRequest("ken@example.com", "correct-password", "Ken", "Thompson"))
        .retrieve()
        .toBodilessEntity();

    HttpClientErrorException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            HttpClientErrorException.class,
            () ->
                client
                    .post()
                    .uri("/api/v1/auth/login")
                    .body(new LoginRequest("ken@example.com", "wrong-password"))
                    .retrieve()
                    .toBodilessEntity());
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
