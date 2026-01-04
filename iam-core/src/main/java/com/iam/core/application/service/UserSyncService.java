package com.iam.core.application.service;

import com.iam.core.application.dto.ProvisioningCommand;
import com.iam.core.application.dto.UserSyncEvent;

import com.iam.core.domain.constant.AttributeConstants;
import com.iam.core.domain.constant.SyncConstants;
import com.iam.core.domain.constant.SystemConstants;
import com.iam.core.domain.entity.*;
import com.iam.core.domain.exception.ErrorCode;
import com.iam.core.domain.exception.IamBusinessException;
import com.iam.core.domain.port.MessagePublisher;
import com.iam.core.domain.repository.IdentityLinkRepository;
import com.iam.core.domain.vo.UniversalData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 동기화 비즈니스 로직을 처리하는 Service
 * Domain Layer - 비즈니스 규칙에만 집중
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final IdentityLinkRepository identityLinkRepository;
    private final MessagePublisher messagePublisher;
    private final SyncHistoryService syncHistoryService;
    private final TransformationService transformationService;
    private final IamUserUpdateService iamUserUpdateService;
    private final IdentityCorrelationService correlationService;

    private static final String PROVISION_ROUTING_KEY = "cmd.ad.user.create";
    private static final String COMPENSATION_ROUTING_KEY = "iam.event.compensation";
    private static final String EXCHANGE_NAME = "iam.topic";

    /**
     * 원천 시스템으로부터 수신한 사용자 동기화 이벤트를 처리
     */
    @Transactional
    public void processSync(UserSyncEvent event) {
        String traceId = event.traceId();
        String systemId = event.systemId();
        log.info("Processing sync for system: {}, traceId: {}", systemId, traceId);

        Map<String, Object> rawData = event.payload();
        String externalId = (String) rawData.get(AttributeConstants.EXTERNAL_ID);
        String userName = (String) rawData.get(AttributeConstants.USERNAME);

        try {
            // 0. Pre-validation: Ensure externalId exists in raw data
            if (externalId == null || externalId.isBlank()) {
                throw new IamBusinessException(ErrorCode.MISSING_REQUIRED_FIELD, traceId,
                        "externalId is missing in raw payload");
            }

            // 1. Transform Attribute using Engine
            var transformationResult = transformationService.transform(systemId, rawData);
            Map<String, UniversalData> transformed = transformationResult.data();
            List<Long> appliedRules = transformationResult.appliedRuleVersionIds();

            // 1.1 Validation: Ensure mandatory fields (like userName) are present after
            // transformation
            validateRequiredAttributes(transformed, traceId);

            // 2. Correlation & Update
            correlationService.correlate(systemId, externalId)
                    .ifPresentOrElse(
                            user -> updateExistingUser(user, transformed, rawData, systemId, traceId,
                                    event.rawMessage(), appliedRules),
                            () -> createNewUser(externalId, transformed, rawData, systemId, traceId,
                                    event.rawMessage(), appliedRules));

        } catch (Exception e) {
            log.error("Sync failed for traceId: {}", traceId, e);
            syncHistoryService.logFailure(traceId, SyncConstants.EVENT_SYNC_ERROR,
                    userName != null ? userName : "UNKNOWN", null, systemId,
                    event.rawMessage() != null ? (Map<String, Object>) event.rawMessage() : rawData,
                    "Error: " + e.getMessage());

            // Publish Compensation Event
            if (externalId != null && !externalId.isBlank()) {
                com.iam.core.domain.event.SyncCompensationEvent compEvent = new com.iam.core.domain.event.SyncCompensationEvent(
                        traceId, systemId, externalId, e.getMessage(), java.time.LocalDateTime.now());
                messagePublisher.publish(EXCHANGE_NAME, COMPENSATION_ROUTING_KEY, compEvent);
                log.info("Published compensation event for traceId: {}, system: {}, externalId: {}", traceId, systemId,
                        externalId);
            }

            if (e instanceof IamBusinessException) {
                throw e;
            }
            throw new IamBusinessException(ErrorCode.MESSAGE_PROCESSING_ERROR, traceId, e.getMessage(), e);
        }
    }

    private void createNewUser(String externalId, Map<String, UniversalData> attributes,
            Map<String, Object> rawData, String systemId, String traceId, Object originalRequest,
            List<Long> appliedRules) {
        log.info("Creating new user via engine for system: {}, ID: {}", systemId, externalId);

        var user = iamUserUpdateService.create(externalId, attributes);
        var link = createIdentityLink(user.getId(), systemId, externalId);
        identityLinkRepository.save(link);

        publishProvisioningCommand(user, extractRawExtensions(attributes), traceId);

        // Normalized History: No more huge mappings nested data
        Map<String, Object> resultSnapshot = extractRawExtensions(attributes);

        syncHistoryService.logSuccess(traceId, SyncConstants.EVENT_USER_CREATE, user.getUserName(), user.getId(),
                systemId,
                resultSnapshot,
                "User created via engine", null, null, rawData, appliedRules);
    }

    private void updateExistingUser(IamUser user, Map<String, UniversalData> attributes,
            Map<String, Object> rawData, String systemId, String traceId, Object originalRequest,
            List<Long> appliedRules) {
        log.info("Updating existing user via engine for system: {}, userId: {}", systemId, user.getId());

        // Capture old state for diff
        Map<String, Object> oldState = captureState(user);

        iamUserUpdateService.update(user, attributes);
        publishProvisioningCommand(user, extractRawExtensions(attributes), traceId);

        // Calculate changes
        List<Map<String, Object>> changes = calculateChanges(oldState, captureState(user));

        // Result Data
        Map<String, Object> resultSnapshot = new HashMap<>();
        resultSnapshot.put(AttributeConstants.CHANGES, changes);
        resultSnapshot.put(AttributeConstants.SYNC_TYPE,
                changes.isEmpty() ? SyncConstants.TYPE_UPDATE_SIMPLE : SyncConstants.TYPE_UPDATE_CRITICAL);

        syncHistoryService.logSuccess(traceId, SyncConstants.EVENT_USER_UPDATE, user.getUserName(), user.getId(),
                systemId,
                resultSnapshot,
                "User updated via engine", null, null, rawData, appliedRules);
    }

    private Map<String, Object> captureState(IamUser user) {
        Map<String, Object> state = new HashMap<>();
        state.put(AttributeConstants.USERNAME, user.getUserName());
        state.put(AttributeConstants.FAMILY_NAME, user.getFamilyName());
        state.put(AttributeConstants.GIVEN_NAME, user.getGivenName());
        state.put(AttributeConstants.TITLE, user.getTitle());
        state.put(AttributeConstants.ACTIVE, user.isActive());
        // Extensions could be added here if needed
        return state;
    }

    private List<Map<String, Object>> calculateChanges(Map<String, Object> oldState, Map<String, Object> newState) {
        List<Map<String, Object>> changes = new ArrayList<>();
        newState.forEach((k, v) -> {
            Object oldVal = oldState.get(k);
            if (v != null && !v.equals(oldVal)) {
                changes.add(Map.of(AttributeConstants.FIELD, k, AttributeConstants.OLD_VALUE, String.valueOf(oldVal),
                        AttributeConstants.NEW_VALUE, String.valueOf(v)));
            }
        });
        return changes;
    }

    private void validateRequiredAttributes(Map<String, UniversalData> attributes, String traceId) {
        UniversalData userNameData = attributes.get(AttributeConstants.USERNAME);
        if (userNameData == null || userNameData.asString().isBlank()) {
            throw new IamBusinessException(ErrorCode.MISSING_REQUIRED_FIELD, traceId,
                    "Mandatory attribute 'userName' is missing after transformation");
        }
    }

    private Map<String, Object> extractRawExtensions(Map<String, UniversalData> attributes) {
        Map<String, Object> raw = new HashMap<>();
        attributes.forEach((k, v) -> raw.put(k, v.getValue()));
        return raw;
    }

    private IdentityLink createIdentityLink(Long iamUserId, String systemType, String externalId) {
        var link = new IdentityLink();
        link.setIamUserId(iamUserId);
        link.setSystemType(systemType);
        link.setExternalId(externalId);
        link.setActive(true);
        return link;
    }

    private void publishProvisioningCommand(IamUser user, Map<String, Object> attributes, String traceId) {
        var command = new ProvisioningCommand(
                traceId,
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
        log.info("Published provisioning command for user: {} (traceId: {})", user.getId(), traceId);

        // Audit Logging for Provisioning
        // Convert command to Map for JSONB storage
        Map<String, Object> cmdMap = new HashMap<>();
        cmdMap.put("target", SystemConstants.SYSTEM_AD);
        cmdMap.put("command", "PROVISION");
        cmdMap.put("payload", command); // Jackson/Hibernate will handle serialization

        syncHistoryService.logSuccess(
                traceId,
                SyncConstants.EVENT_AD_PROVISION,
                user.getUserName(),
                user.getId(),
                SystemConstants.SYSTEM_IAM,
                cmdMap,
                "Provisioning request sent to AD",
                null,
                null,
                null,
                null);
    }
}
