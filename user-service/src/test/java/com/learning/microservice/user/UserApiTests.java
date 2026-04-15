package com.learning.microservice.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.microservice.user.web.dto.CreateUserRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

class UserApiTests extends AbstractIntegrationTest {

  @LocalServerPort int port;
  @Autowired RestClient.Builder clientBuilder;

  @Test
  void createAndFetchUser() {
    RestClient client = clientBuilder.baseUrl("http://localhost:" + port).build();

    var create = new CreateUserRequest("ada@example.com", "super-secret", "Ada", "Lovelace");

    ResponseEntity<Map<String, Object>> created =
        client
            .post()
            .uri("/api/v1/users")
            .body(create)
            .retrieve()
            .toEntity(new org.springframework.core.ParameterizedTypeReference<>() {});

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) created.getBody().get("data");
    String id = (String) data.get("id");
    assertThat(id).isNotBlank();
    assertThat(data.get("email")).isEqualTo("ada@example.com");

    ResponseEntity<Map<String, Object>> fetched =
        client
            .get()
            .uri("/api/v1/users/{id}", id)
            .retrieve()
            .toEntity(new org.springframework.core.ParameterizedTypeReference<>() {});

    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) fetched.getBody().get("data");
    assertThat(body.get("firstName")).isEqualTo("Ada");
    // Password hash must never leak out in the response.
    assertThat(body).doesNotContainKey("passwordHash");
    assertThat(body).doesNotContainKey("password");
  }
}
