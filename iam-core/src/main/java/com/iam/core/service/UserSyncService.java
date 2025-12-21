package com.iam.core.service;

import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.IdentityLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final IamUserRepository iamUserRepository;
    private final IdentityLinkRepository identityLinkRepository;

    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    // Constant for routing key - in real app could be in properties
    private static final String PROVISION_ROUTING_KEY = "cmd.ad.user.create";
    private static final String EXCHANGE_NAME = "iam.topic";

    @Transactional
    @RabbitListener(queues = "q.iam.core.ingest")
    public void processHrSync(Map<String, Object> message) {
        log.info("Received sync event: {}", message);

        String hrEmpId = com.iam.core.util.AttributeUtils.getString((Map<String, Object>) message.get("payload"),
                "hrEmpId");
        if (hrEmpId.isEmpty()) {
            log.warn("Ignoring event with no hrEmpId");
            return;
        }

        identityLinkRepository.findBySystemTypeAndExternalId("HR", hrEmpId).ifPresentOrElse(
                link -> updateExistingUser(link, message),
                () -> createNewUser(hrEmpId, message));
    }

    private void createNewUser(String hrEmpId, Map<String, Object> message) {
        log.info("Creating new user for HR ID: {}", hrEmpId);
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        String name = com.iam.core.util.AttributeUtils.getString(payload, "name");
        Map<String, Object> attributes = (Map<String, Object>) payload.get("attributes");

        // 1. Core User
        com.iam.core.domain.entity.IamUser user = new com.iam.core.domain.entity.IamUser();
        user.setId(java.util.UUID.randomUUID().toString());
        user.setLoginId(hrEmpId); // Simplification for MVP
        user.setName(name);
        user.setStatus(com.iam.core.domain.entity.UserStatus.ACTIVE);

        // 2. Extension
        com.iam.core.domain.entity.IamUserExtension ext = new com.iam.core.domain.entity.IamUserExtension();
        ext.setUser(user);
        ext.setAttributes(attributes != null ? attributes : new java.util.HashMap<>());
        user.setExtension(ext);

        iamUserRepository.save(user);

        // 3. Identity Link
        com.iam.core.domain.entity.IdentityLink link = new com.iam.core.domain.entity.IdentityLink();
        link.setIamUserId(user.getId());
        link.setSystemType("HR");
        link.setExternalId(hrEmpId);
        link.setActive(true);
        identityLinkRepository.save(link);

        // 4. Provisioning Trigger
        publishProvisioningCommand(user, attributes);
    }

    private void updateExistingUser(com.iam.core.domain.entity.IdentityLink link, Map<String, Object> message) {
        log.info("Updating existing user for HR ID: {}", link.getExternalId());
        // Logic for update (omitted for brevity as per prompt focus on structure, but
        // can add if requested)
        // For MVP, if we want to update name or attributes:
        iamUserRepository.findById(link.getIamUserId()).ifPresent(user -> {
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            String newName = com.iam.core.util.AttributeUtils.getString(payload, "name");
            if (!newName.isEmpty() && !newName.equals(user.getName())) {
                user.setName(newName);
            }

            // Update Extension
            Map<String, Object> newAttrs = (Map<String, Object>) payload.get("attributes");
            if (newAttrs != null && user.getExtension() != null) {
                user.getExtension().getAttributes().putAll(newAttrs);
            }
            // JPA Dirty checking will save changes
        });
    }

    private void publishProvisioningCommand(com.iam.core.domain.entity.IamUser user, Map<String, Object> attributes) {
        // Construct Command
        Map<String, Object> command = new java.util.HashMap<>();
        command.put("command", "CREATE_ACCOUNT");

        // Wrap data in 'payload' to match Connector expectation
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("targetSystemId", user.getLoginId());
        payload.put("attributes", attributes);

        command.put("payload", payload);

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, PROVISION_ROUTING_KEY, command);
        log.info("Published provisioning command for user: {} with payload mapping", user.getId());
    }
}
