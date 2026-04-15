package com.learning.microservice.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.microservice.common.event.DomainEvents;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Declares the shared domain.events topic exchange and a JSON converter so RabbitTemplate
// publishes OrderCreatedV1 as JSON (decision #12 + #15).
@Configuration
public class RabbitConfig {

  @Bean
  TopicExchange domainEventsExchange() {
    return new TopicExchange(DomainEvents.EXCHANGE, true, false);
  }

  @Bean
  MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }
}
