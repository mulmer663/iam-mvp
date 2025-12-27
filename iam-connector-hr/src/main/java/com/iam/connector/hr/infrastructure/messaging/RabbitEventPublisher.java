package com.iam.connector.hr.infrastructure.messaging;

import com.iam.connector.hr.application.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE_NAME = "iam.topic";
    private static final String ROUTING_KEY_PREFIX = "hr.event.";

    @Override
    public void publish(String systemId, Map<String, Object> payload) {
        Map<String, Object> event = new HashMap<>();
        String traceId = UUID.randomUUID().toString();

        event.put("traceId", traceId);
        event.put("systemId", systemId);
        event.put("eventType", "USER_SYNC");
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("payload", payload);

        String routingKey = ROUTING_KEY_PREFIX + "user.sync." + systemId.toLowerCase();

        log.info("Publishing HR event: traceId={}, systemId={}, routingKey={}", traceId, systemId, routingKey);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, event);
    }
}
