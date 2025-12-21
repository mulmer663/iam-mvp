package com.iam.connector.hr.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrIngestController {

    private final RabbitTemplate rabbitTemplate;

    // Configured in Core, but we use it here too
    private static final String EXCHANGE_NAME = "iam.topic";
    private static final String ROUTING_KEY = "hr.event.user.sync";

    @PostMapping("/sync")
    public String receiveHrData(@RequestBody Map<String, Object> payload) {
        log.info("Received HR data via HTTP: {}", payload);

        // Wrap in event envelope if not already (Simulating Connector logic)
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("traceId", UUID.randomUUID().toString());
        event.put("eventType", "SYNC_USER");
        event.put("timestamp", java.time.LocalDateTime.now().toString());
        event.put("payload", payload); // Assuming input is the raw 'payload' part of the spec

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, event);
        return "Event Published: " + event.get("traceId");
    }
}
