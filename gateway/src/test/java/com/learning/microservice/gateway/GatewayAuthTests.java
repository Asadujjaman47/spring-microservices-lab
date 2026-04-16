package com.learning.microservice.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayAuthTests {

  @LocalServerPort int port;

  @Value("${common.jwt.secret}")
  String jwtSecret;

  @Autowired RestClient.Builder clientBuilder;

  @TestConfiguration
  static class StubBackendConfig {
    @Bean
    StubBackendController stubBackendController() {
      return new StubBackendController();
    }
  }

  @RestController
  @RequestMapping("/api/v1/users")
  static class StubBackendController {
    @GetMapping("/whoami")
    String whoami() {
      return "pong";
    }
  }

  @Test
  void protectedPathWithoutTokenIs401() {
    RestClient client = clientBuilder.baseUrl("http://localhost:" + port).build();

    HttpClientErrorException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            HttpClientErrorException.class,
            () -> client.get().uri("/api/v1/users/whoami").retrieve().toBodilessEntity());
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void protectedPathWithValidTokenIsAllowedThrough() {
    RestClient client = clientBuilder.baseUrl("http://localhost:" + port).build();
    String token = signToken();

    String body =
        client
            .get()
            .uri("/api/v1/users/whoami")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .body(String.class);
    assertThat(body).isEqualTo("pong");
  }

  @Test
  void authLoginPathIsPublic() {
    // Filter-allowlist behavior only — no downstream is wired in the test, so the request will
    // fall through to a 404/405/500. The contract we care about is that it is never 401.
    RestClient client = clientBuilder.baseUrl("http://localhost:" + port).build();

    HttpStatusCodeException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            HttpStatusCodeException.class,
            () -> client.get().uri("/api/v1/auth/login").retrieve().toBodilessEntity());
    assertThat(ex.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void openApiDocPathIsPublic() {
    // Aggregated Swagger fetches each service's spec via /v3/api-docs/{service}. The allowlist
    // must let these through unauthenticated — discovery is disabled in tests so the actual
    // route won't resolve, but the contract is "never 401".
    RestClient client = clientBuilder.baseUrl("http://localhost:" + port).build();

    HttpStatusCodeException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            HttpStatusCodeException.class,
            () -> client.get().uri("/v3/api-docs/user-service").retrieve().toBodilessEntity());
    assertThat(ex.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private String signToken() {
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .subject("11111111-1111-1111-1111-111111111111")
        .issuedAt(new Date(now))
        .expiration(new Date(now + 60_000))
        .signWith(key)
        .compact();
  }
}
