package com.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BackendApplicationTests {

  @Container @ServiceConnection
  static MySQLContainer mysql =
      new MySQLContainer("mysql:8.0.31")
          .withDatabaseName("ecommerce_test_db")
          .withUsername("test_user")
          .withPassword("test_password");

  @Test
  void contextLoads() {
    // Verifies Spring Boot, MySQL, Flyway and JPA startup.
  }
}
