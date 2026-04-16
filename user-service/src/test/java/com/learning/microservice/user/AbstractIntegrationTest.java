package com.learning.microservice.user;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

// Postgres container is started once per JVM and shared across every test class. Using
// @Testcontainers/@Container with a static container works for a single test class, but when
// multiple test classes extend this base, the framework stops the container between classes —
// leaving Spring's cached context pointing at a dead JDBC URL. A plain static initializer + no
// JUnit-managed lifecycle gives us a true singleton; Ryuk handles shutdown at JVM exit.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.3-alpine");

  static {
    POSTGRES.start();
  }
}
