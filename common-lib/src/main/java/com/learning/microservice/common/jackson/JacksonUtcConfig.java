package com.learning.microservice.common.jackson;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Forces ISO-8601 UTC for every {@code Instant} / {@code OffsetDateTime} serialized. */
@Configuration
public class JacksonUtcConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer utcJacksonCustomizer() {
    return builder ->
        builder
            .modules(new JavaTimeModule())
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .timeZone(TimeZone.getTimeZone("UTC"));
  }
}
