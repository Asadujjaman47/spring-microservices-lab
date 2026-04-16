package com.learning.microservice.gateway.security;

import com.learning.microservice.common.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class GatewaySecurityConfig {

  @Bean
  public GatewayAuthEnforcementFilter gatewayAuthEnforcementFilter() {
    return new GatewayAuthEnforcementFilter();
  }

  @Bean
  public FilterRegistrationBean<GatewayAuthEnforcementFilter> gatewayAuthEnforcementFilterReg(
      GatewayAuthEnforcementFilter filter) {
    FilterRegistrationBean<GatewayAuthEnforcementFilter> reg = new FilterRegistrationBean<>(filter);
    reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    return reg;
  }

  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterReg(
      JwtAuthenticationFilter filter) {
    FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
    reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return reg;
  }
}
