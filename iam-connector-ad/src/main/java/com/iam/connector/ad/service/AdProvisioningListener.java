package com.iam.connector.ad.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class AdProvisioningListener {

    // Configured in Core, we listen to the queue
    private static final String QUEUE_NAME = "q.iam.connector.ad";

    @RabbitListener(queues = QUEUE_NAME)
    public void processProvisioningCommand(Map<String, Object> command) {
        log.info("Received Provisioning Command: {}", command);

        String operation = (String) command.get("command");
        Map<String, Object> payload = (Map<String, Object>) command.get("payload");

        if ("CREATE_ACCOUNT".equals(operation)) {
            String targetId = (String) payload.get("targetSystemId");
            log.info("Provisioning AD Account for: {}", targetId);
            log.info("Attributes: CN={}, DisplayName={}, FamilyName={}, GivenName={}, Active={}",
                    targetId,
                    payload.get("formattedName"),
                    payload.get("familyName"),
                    payload.get("givenName"),
                    payload.get("active"));
            // Mock LDAP Call
            log.info("LDAP: create_user(cn={}, attributes={})", targetId, payload.get("attributes"));
        } else {
            log.warn("Unknown command: {}", operation);
        }
    }
}
