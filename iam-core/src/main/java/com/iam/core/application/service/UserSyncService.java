package com.iam.core.application.service;

import com.iam.core.domain.entity.EnterpriseUserExtension;
import com.iam.core.domain.entity.ExtensionData;
import com.iam.core.domain.entity.GenericExtension;
import com.iam.core.domain.entity.IamUser;
import com.iam.core.domain.entity.IamUserExtension;
import com.iam.core.domain.entity.IdentityLink;
import com.iam.core.domain.port.MessagePublisher;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.IdentityLinkRepository;
import com.iam.core.application.dto.ProvisioningCommand;
import com.iam.core.application.dto.UserSyncEvent;
import com.iam.core.application.dto.UserSyncPayload;
import com.iam.core.domain.exception.ErrorCode;
import com.iam.core.domain.exception.IamBusinessException;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 사용자 동기화 비즈니스 로직을 처리하는 Service
 * Domain Layer - 비즈니스 규칙에만 집중
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final IamUserRepository iamUserRepository;
    private final IdentityLinkRepository identityLinkRepository;
    private final MessagePublisher messagePublisher;
    private final SyncHistoryService syncHistoryService;

    private static final String PROVISION_ROUTING_KEY = "cmd.ad.user.create";
    private static final String EXCHANGE_NAME = "iam.topic";
    private static final String ENTERPRISE_URN = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

    /**
     * HR 시스템으로부터 수신한 사용자 동기화 이벤트를 처리
     */
    @Transactional
    public void processHrSync(UserSyncEvent event) {
        String traceId = event.traceId();
        log.info("Processing HR sync for traceId: {}", traceId);

        var payload = event.payload();

        try {
            identityLinkRepository.findBySystemTypeAndExternalId("HR", payload.getExternalId())
                    .ifPresentOrElse(
                            link -> updateExistingUser(link, payload, traceId),
                            () -> createNewUser(payload, traceId));
        } catch (DataAccessException e) {
            syncHistoryService.logFailure(traceId, "HR_SYNC", payload.getUserName(), payload,
                    "Database error: " + e.getMessage());
            throw new IamBusinessException(
                    ErrorCode.DATABASE_ERROR,
                    traceId,
                    "사용자 동기화 중 데이터베이스 오류가 발생했습니다",
                    e);
        } catch (Exception e) {
            String message = "Unexpected error: " + e.getMessage();
            syncHistoryService.logFailure(traceId, "HR_SYNC", payload.getUserName(), payload, message);

            if (e instanceof IamBusinessException) {
                throw e; // 이미 처리된 비즈니스 예외는 그대로 전파
            }
            throw new IamBusinessException(
                    ErrorCode.MESSAGE_PROCESSING_ERROR,
                    traceId,
                    message,
                    e);
        }
    }

    private void createNewUser(UserSyncPayload payload, String traceId) {
        log.info("Creating new user for HR ID: {}", payload.getExternalId());

        var user = buildNewUser(payload);
        iamUserRepository.save(user);

        var link = createIdentityLink(user.getId(), payload.getExternalId());
        identityLinkRepository.save(link);

        publishProvisioningCommand(user, payload.getExtensions());

        syncHistoryService.logSuccess(traceId, "HR_SYNC", user.getUserName(), payload, "User created successfully");
        log.info("Successfully created user with ID: {}", user.getId());
    }

    private void updateExistingUser(IdentityLink link, UserSyncPayload payload, String traceId) {
        log.info("Updating existing user for HR ID: {}", link.getExternalId());

        iamUserRepository.findById(link.getIamUserId())
                .ifPresentOrElse(
                        user -> {
                            updateUserFields(user, payload);
                            updateUserExtensions(user, payload);
                            iamUserRepository.save(user);
                            publishProvisioningCommand(user, payload.getExtensions());

                            syncHistoryService.logSuccess(traceId, "USER_UPDATE", user.getUserName(), payload,
                                    "User updated successfully");
                            log.info("Successfully updated user with ID: {}", user.getId());
                        },
                        () -> {
                            String msg = "IdentityLink exists but User not found for ID: " + link.getIamUserId();
                            syncHistoryService.logFailure(traceId, "USER_UPDATE", payload.getUserName(), payload, msg);
                            throw new IamBusinessException(
                                    ErrorCode.USER_NOT_FOUND,
                                    traceId,
                                    msg);
                        });
    }

    private IamUser buildNewUser(UserSyncPayload payload) {
        var user = new IamUser();
        user.setId(TSID.fast().toLong());
        user.setExternalId(payload.getExternalId());
        updateUserFields(user, payload);
        user.setResourceType("User");
        user.setCreated(LocalDateTime.now());
        user.setLastModified(LocalDateTime.now());

        var extension = buildUserExtension(user, payload);
        user.setExtension(extension);

        return user;
    }

    private IamUserExtension buildUserExtension(IamUser user, UserSyncPayload payload) {
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
        return ext;
    }

    private void updateUserFields(IamUser user, UserSyncPayload payload) {
        user.setUserName(payload.getUserName());
        if (payload.getName() != null) {
            user.setFamilyName(payload.getName().getFamilyName());
            user.setGivenName(payload.getName().getGivenName());
            user.setFormattedName(payload.getName().getFormatted());
        }
        user.setTitle(payload.getTitle());
        user.setActive(payload.getActive());
        user.setLastModified(LocalDateTime.now());
    }

    private void updateUserExtensions(IamUser user, UserSyncPayload payload) {
        if (payload.getExtensions() != null && user.getExtension() != null) {
            var ext = user.getExtension();
            payload.getExtensions().forEach((urn, data) -> {
                if (!ext.getSchemas().contains(urn)) {
                    ext.getSchemas().add(urn);
                }
                ext.getExtensions().put(urn, mapToExtensionData(urn, data));
            });
        }
    }

    private IdentityLink createIdentityLink(Long iamUserId, String externalId) {
        var link = new IdentityLink();
        link.setIamUserId(iamUserId);
        link.setSystemType("HR");
        link.setExternalId(externalId);
        link.setActive(true);
        return link;
    }

    private ExtensionData mapToExtensionData(String urn, Object data) {
        if (ENTERPRISE_URN.equals(urn) && data instanceof Map<?, ?> map) {
            return mapToEnterpriseExtension(map);
        } else {
            return mapToGenericExtension(data);
        }
    }

    private EnterpriseUserExtension mapToEnterpriseExtension(Map<?, ?> map) {
        var enterprise = new EnterpriseUserExtension();
        enterprise.setEmployeeNumber(asString(map.get("employeeNumber")));
        enterprise.setDepartment(asString(map.get("department")));
        enterprise.setCostCenter(asString(map.get("costCenter")));
        enterprise.setOrganization(asString(map.get("organization")));
        enterprise.setDivision(asString(map.get("division")));
        return enterprise;
    }

    private GenericExtension mapToGenericExtension(Object data) {
        var generic = new GenericExtension();
        if (data instanceof Map<?, ?> map) {
            map.forEach((k, v) -> generic.add(k.toString(), v));
        }
        return generic;
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

        messagePublisher.publish(EXCHANGE_NAME, PROVISION_ROUTING_KEY, command);
        log.info("Published provisioning command for user: {}", user.getId());
    }
}
