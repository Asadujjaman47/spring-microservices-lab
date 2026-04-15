package com.learning.microservice.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  // @LoadBalanced makes the builder resolve service IDs (e.g. http://product-service)
  // through Spring Cloud LoadBalancer + Eureka, instead of treating the host as a DNS name.
  @Bean
  @LoadBalanced
  RestClient.Builder loadBalancedRestClientBuilder() {
    return RestClient.builder();
  }
}
