package com.learning.microservice.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.microservice.common.event.DomainEvents;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Consumer topology for OrderCreatedV1:
//   domain.events (topic)  ──order.created──▶  notification.order.created.q
//                                                  │ (on retry exhaustion, requeue=false)
//                                                  ▼
//                                         domain.events.dlx (direct)
//                                                  │
//                                                  ▼
//                                     notification.order.created.dlq
@Configuration
public class RabbitConfig {

  public static final String ORDER_CREATED_QUEUE = "notification.order.created.q";
  public static final String ORDER_CREATED_DLQ = "notification.order.created.dlq";
  public static final String DLX_EXCHANGE = "domain.events.dlx";

  @Bean
  TopicExchange domainEventsExchange() {
    return new TopicExchange(DomainEvents.EXCHANGE, true, false);
  }

  @Bean
  DirectExchange deadLetterExchange() {
    return new DirectExchange(DLX_EXCHANGE, true, false);
  }

  @Bean
  Queue orderCreatedQueue() {
    return QueueBuilder.durable(ORDER_CREATED_QUEUE)
        .withArguments(
            Map.of(
                "x-dead-letter-exchange", DLX_EXCHANGE,
                "x-dead-letter-routing-key", ORDER_CREATED_DLQ))
        .build();
  }

  @Bean
  Queue orderCreatedDlq() {
    return QueueBuilder.durable(ORDER_CREATED_DLQ).build();
  }

  @Bean
  Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange domainEventsExchange) {
    return BindingBuilder.bind(orderCreatedQueue)
        .to(domainEventsExchange)
        .with(DomainEvents.RoutingKeys.ORDER_CREATED);
  }

  @Bean
  Binding orderCreatedDlqBinding(Queue orderCreatedDlq, DirectExchange deadLetterExchange) {
    return BindingBuilder.bind(orderCreatedDlq).to(deadLetterExchange).with(ORDER_CREATED_DLQ);
  }

  @Bean
  MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }
}
