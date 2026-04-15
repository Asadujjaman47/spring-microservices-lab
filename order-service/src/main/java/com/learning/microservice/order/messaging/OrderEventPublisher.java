package com.learning.microservice.order.messaging;

import com.learning.microservice.common.event.DomainEvents;
import com.learning.microservice.common.event.OrderCreatedV1;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

  private final RabbitTemplate rabbit;

  public OrderEventPublisher(RabbitTemplate rabbit) {
    this.rabbit = rabbit;
  }

  public void publishOrderCreated(OrderCreatedV1 event) {
    rabbit.convertAndSend(DomainEvents.EXCHANGE, DomainEvents.RoutingKeys.ORDER_CREATED, event);
  }
}
