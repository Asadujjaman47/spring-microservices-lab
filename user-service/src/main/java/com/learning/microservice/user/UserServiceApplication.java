package com.learning.microservice.user;

import com.learning.microservice.user.service.AuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// @EnableJpaAuditing activates BaseEntity's @CreatedDate / @LastModifiedDate / @CreatedBy /
// @LastModifiedBy.
@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(AuthProperties.class)
public class UserServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(UserServiceApplication.class, args);
  }
}
