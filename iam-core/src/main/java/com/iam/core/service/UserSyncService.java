package com.iam.core.service;

import io.hypersistence.tsid.TSID;
import com.iam.core.domain.entity.*;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.IdentityLinkRepository;
import com.iam.core.dto.ProvisioningCommand;
import com.iam.core.dto.UserSyncEvent;
import com.iam.core.dto.UserSyncPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final IamUserRepository iamUserRepository;
    private final IdentityLinkRepository identityLinkRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final String PROVISION_ROUTING_KEY = "cmd.ad.user.create";
    private static final String EXCHANGE_NAME = "iam.topic";
    private static final String ENTERPRISE_URN = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

    @Transactional
    @RabbitListener(queues = "q.iam.core.ingest")
    public void processHrSync(UserSyncEvent event) {
        log.info("Received sync event traceId: {}", event.traceId());

        var payload = event.payload();
        if (payload == null || payload.getExternalId() == null || payload.getExternalId().isBlank()) {
            log.warn("Ignoring event with invalid payload or externalId");
            return;
        }

        identityLinkRepository.findBySystemTypeAndExternalId("HR", payload.getExternalId()).ifPresentOrElse(
                link -> updateExistingUser(link, payload),
                () -> createNewUser(payload));
    }

    private void createNewUser(UserSyncPayload payload) {
        log.info("Creating new user for HR ID: {}", payload.getExternalId());

        var user = new IamUser();
        user.setId(TSID.fast().toLong());
        user.setExternalId(payload.getExternalId());
        user.setUserName(payload.getUserName());
        if (payload.getName() != null) {
            user.setFamilyName(payload.getName().getFamilyName());
            user.setGivenName(payload.getName().getGivenName());
            user.setFormattedName(payload.getName().getFormatted());
        }
        user.setTitle(payload.getTitle());
        user.setActive(payload.isActive());
        user.setResourceType("User");
        user.setCreated(LocalDateTime.now());
        user.setLastModified(LocalDateTime.now());

        var ext = new IamUserExtension();
        ext.setUser(user);
        ext.setSchemas(new ArrayList<>(List.of("urn:ietf:params:scim:schemas:core:2.0:User")));

        if (payload.getExtensions() != null) {
            payload.getExtensions().forEach((urn, data) -> {
                if (!ext.getSchemas().contains(urn)) {
                    ext.getSchemas().add(urn);
                }
                ext.getExtensions().put(urn, mapToExtensionData(urn, data));
            });
        }
        user.setExtension(ext);

        iamUserRepository.save(user);

        var link = new IdentityLink();
        link.setIamUserId(user.getId());
        link.setSystemType("HR");
        link.setExternalId(payload.getExternalId());
        link.setActive(true);
        identityLinkRepository.save(link);

        publishProvisioningCommand(user, payload.getExtensions());
    }

    private void updateExistingUser(IdentityLink link, UserSyncPayload payload) {
        log.info("Updating existing user for HR ID: {}", link.getExternalId());

        iamUserRepository.findById(link.getIamUserId()).ifPresent(user -> {
            user.setUserName(payload.getUserName());
            if (payload.getName() != null) {
                user.setFamilyName(payload.getName().getFamilyName());
                user.setGivenName(payload.getName().getGivenName());
                user.setFormattedName(payload.getName().getFormatted());
            }
            user.setTitle(payload.getTitle());
            user.setActive(payload.isActive());
            user.setLastModified(LocalDateTime.now());

            if (payload.getExtensions() != null && user.getExtension() != null) {
                var ext = user.getExtension();
                payload.getExtensions().forEach((urn, data) -> {
                    if (!ext.getSchemas().contains(urn)) {
                        ext.getSchemas().add(urn);
                    }
                    ext.getExtensions().put(urn, mapToExtensionData(urn, data));
                });
            }
        });
    }

    private ExtensionData mapToExtensionData(String urn, Object data) {
        if (ENTERPRISE_URN.equals(urn) && data instanceof Map<?, ?> map) {
            var enterprise = new EnterpriseUserExtension();
            enterprise.setEmployeeNumber(asString(map.get("employeeNumber")));
            enterprise.setDepartment(asString(map.get("department")));
            enterprise.setCostCenter(asString(map.get("costCenter")));
            enterprise.setOrganization(asString(map.get("organization")));
            enterprise.setDivision(asString(map.get("division")));
            return enterprise;
        } else {
            var generic = new GenericExtension();
            if (data instanceof Map<?, ?> map) {
                map.forEach((k, v) -> generic.add(k.toString(), v));
            }
            return generic;
        }
    }

    private String asString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private void publishProvisioningCommand(IamUser user, Map<String, Object> attributes) {
        var command = new ProvisioningCommand(
                UUID.randomUUID().toString(),
                "CAUSE_EVENT_ID",
                "CREATE_ACCOUNT",
                new ProvisioningCommand.ProvisioningPayload(
                        user.getUserName(),
                        user.getFamilyName(),
                        user.getGivenName(),
                        user.getFormattedName(),
                        user.isActive(),
                        attributes));

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, PROVISION_ROUTING_KEY, command);
        log.info("Published provisioning command for user: {}", user.getId());
    }
}
