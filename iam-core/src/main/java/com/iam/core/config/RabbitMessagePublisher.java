package com.iam.core.config;

import com.iam.core.domain.port.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ를 이용한 MessagePublisher 구현체
 * Infrastructure Layer에서 Domain Layer의 포트를 구현
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMessagePublisher implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String DEFAULT_EXCHANGE = "iam.topic";

    @Override
    public void publish(String routingKey, Object message) {
        publish(DEFAULT_EXCHANGE, routingKey, message);
    }

    @Override
    public void publish(String exchange, String routingKey, Object message) {
        try {
            log.debug("Publishing message to exchange: {}, routingKey: {}, message: {}",
                    exchange, routingKey, message);
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("Message published successfully to exchange: {}, routingKey: {}",
                    exchange, routingKey);
        } catch (Exception e) {
            log.error("Failed to publish message to exchange: {}, routingKey: {}",
                    exchange, routingKey, e);
            throw new RuntimeException("메시지 발행 실패", e);
        }
    }
}