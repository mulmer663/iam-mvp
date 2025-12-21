package com.iam.core.service;

import io.hypersistence.tsid.TSID;
import com.iam.core.domain.entity.IamUser;
import com.iam.core.domain.entity.IamUserExtension;
import com.iam.core.domain.entity.IdentityLink;
import com.iam.core.domain.entity.UserStatus;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.IdentityLinkRepository;
import com.iam.core.dto.ProvisioningCommand;
import com.iam.core.dto.UserSyncEvent;
import com.iam.core.dto.UserSyncPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

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
    public void processHrSync(UserSyncEvent event) {
        log.info("Received sync event traceId: {}", event.traceId());

        var payload = event.payload();
        if (payload == null || payload.hrEmpId() == null || payload.hrEmpId().isBlank()) {
            log.warn("Ignoring event with invalid payload or hrEmpId");
            return;
        }

        identityLinkRepository.findBySystemTypeAndExternalId("HR", payload.hrEmpId()).ifPresentOrElse(
                link -> updateExistingUser(link, payload),
                () -> createNewUser(payload));
    }

    private void createNewUser(UserSyncPayload payload) {
        log.info("Creating new user for HR ID: {}", payload.hrEmpId());

        // 1. Core User
        var user = new IamUser();
        user.setId(TSID.fast().toLong());
        user.setLoginId(payload.hrEmpId()); // Simplification for MVP
        user.setName(payload.name());
        user.setStatus(UserStatus.ACTIVE);

        // 2. Extension
        var ext = new IamUserExtension();
        ext.setUser(user);
        ext.setAttributes(payload.attributes() != null ? new HashMap<>(payload.attributes()) : new HashMap<>());
        user.setExtension(ext);

        iamUserRepository.save(user);

        // 3. Identity Link
        var link = new IdentityLink();
        link.setIamUserId(user.getId());
        link.setSystemType("HR");
        link.setExternalId(payload.hrEmpId());
        link.setActive(true);
        identityLinkRepository.save(link);

        // 4. Provisioning Trigger
        publishProvisioningCommand(user, payload.attributes());
    }

    private void updateExistingUser(IdentityLink link, UserSyncPayload payload) {
        log.info("Updating existing user for HR ID: {}", link.getExternalId());

        iamUserRepository.findById(link.getIamUserId()).ifPresent(user -> {
            if (payload.name() != null && !payload.name().isBlank() && !payload.name().equals(user.getName())) {
                user.setName(payload.name());
            }

            // Update Extension
            if (payload.attributes() != null && user.getExtension() != null) {
                user.getExtension().getAttributes().putAll(payload.attributes());
            }
        });
    }

    private void publishProvisioningCommand(IamUser user, java.util.Map<String, Object> attributes) {
        var command = new ProvisioningCommand(
                java.util.UUID.randomUUID().toString(), // Keep traceId as String for now
                "CAUSE_EVENT_ID",
                "CREATE_ACCOUNT",
                new ProvisioningCommand.ProvisioningPayload(
                        user.getLoginId(),
                        attributes));

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, PROVISION_ROUTING_KEY, command);
        log.info("Published provisioning command for user: {}", user.getId());
    }
}
