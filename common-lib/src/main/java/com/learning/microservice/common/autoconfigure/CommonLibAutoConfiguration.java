package com.learning.microservice.common.autoconfigure;

import com.learning.microservice.common.error.GlobalExceptionHandler;
import com.learning.microservice.common.jackson.JacksonUtcConfig;
import com.learning.microservice.common.security.JwtAuthenticationFilter;
import com.learning.microservice.common.security.JwtProperties;
import com.learning.microservice.common.tracing.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Services that depend on {@code common-lib} automatically get:
 *
 * <ul>
 *   <li>the correlation-id filter on every request,
 *   <li>the global exception advice mapping to {@link
 *       com.learning.microservice.common.api.ApiResponse},
 *   <li>UTC / ISO-8601 Jackson defaults,
 *   <li>the JWT filter when {@code common.jwt.enabled=true}.
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@Import({GlobalExceptionHandler.class, JacksonUtcConfig.class})
public class CommonLibAutoConfiguration {

  @Bean
  public CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Bean
  @ConditionalOnProperty(prefix = "common.jwt", name = "enabled", havingValue = "true")
  public JwtAuthenticationFilter jwtAuthenticationFilter(JwtProperties props) {
    return new JwtAuthenticationFilter(props);
  }
}
