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
import java.util.UUID;

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
            // 1. Transform Attribute using Engine
            Map<String, UniversalData> transformed = transformationService.transform(systemId,
                    rawData);

            // 2. Correlation & Update
            correlationService.correlate(systemId, externalId)
                    .ifPresentOrElse(
                            user -> updateExistingUser(user, transformed, rawData, systemId, traceId),
                            () -> createNewUser(externalId, transformed, rawData, systemId, traceId));

        } catch (Exception e) {
            log.error("Sync failed for traceId: {}", traceId, e);
            syncHistoryService.logFailure(traceId, SyncConstants.EVENT_SYNC_ERROR,
                    userName != null ? userName : "UNKNOWN", null, systemId,
                    rawData,
                    "Error: " + e.getMessage());

            if (e instanceof IamBusinessException) {
                throw e;
            }
            throw new IamBusinessException(ErrorCode.MESSAGE_PROCESSING_ERROR, traceId, e.getMessage(), e);
        }
    }

    private void createNewUser(String externalId, Map<String, UniversalData> attributes,
            Map<String, Object> rawData, String systemId, String traceId) {
        log.info("Creating new user via engine for system: {}, ID: {}", systemId, externalId);

        var user = iamUserUpdateService.create(externalId, attributes);
        var link = createIdentityLink(user.getId(), systemId, externalId);
        identityLinkRepository.save(link);

        publishProvisioningCommand(user, extractRawExtensions(attributes));

        // Rich History
        Map<String, Object> historyPayload = Map.of(
                AttributeConstants.SYNC_TYPE, SyncConstants.TYPE_JOIN,
                AttributeConstants.MAPPINGS, generateMappings(systemId, attributes),
                AttributeConstants.SNAPSHOT, Map.of(
                        AttributeConstants.LAYER, systemId,
                        AttributeConstants.DATA, rawData));

        syncHistoryService.logSuccess(traceId, SyncConstants.EVENT_USER_CREATE, user.getUserName(), user.getId(),
                systemId,
                historyPayload,
                "User created via engine");
    }

    private void updateExistingUser(IamUser user, Map<String, UniversalData> attributes,
            Map<String, Object> rawData, String systemId, String traceId) {
        log.info("Updating existing user via engine for system: {}, userId: {}", systemId, user.getId());

        // Capture old state for diff
        Map<String, Object> oldState = captureState(user);

        iamUserUpdateService.update(user, attributes);
        publishProvisioningCommand(user, extractRawExtensions(attributes));

        // Calculate changes
        List<Map<String, Object>> changes = calculateChanges(oldState, captureState(user));

        // Rich History
        Map<String, Object> historyPayload = new HashMap<>();
        historyPayload.put(AttributeConstants.SYNC_TYPE,
                changes.isEmpty() ? SyncConstants.TYPE_UPDATE_SIMPLE : SyncConstants.TYPE_UPDATE_CRITICAL);
        historyPayload.put(AttributeConstants.CHANGES, changes);
        historyPayload.put(AttributeConstants.MAPPINGS, generateMappings(systemId, attributes));
        historyPayload.put(AttributeConstants.SNAPSHOT,
                Map.of(AttributeConstants.LAYER, systemId, AttributeConstants.DATA, rawData));

        syncHistoryService.logSuccess(traceId, SyncConstants.EVENT_USER_UPDATE, user.getUserName(), user.getId(),
                systemId,
                historyPayload, "User updated via engine");
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

    private List<Map<String, Object>> generateMappings(String from,
            Map<String, UniversalData> attributes) {
        List<Map<String, Object>> mappings = new ArrayList<>();
        attributes.forEach((k, v) -> {
            mappings.add(Map.of(
                    AttributeConstants.FROM_LABEL, from,
                    AttributeConstants.TO_LABEL, SystemConstants.SYSTEM_IAM,
                    AttributeConstants.FROM_FIELD, k, // For now assuming 1:1 name for simplicity if not traceable
                    AttributeConstants.TO_FIELD, k,
                    AttributeConstants.VALUE, v.asString()));
        });
        return mappings;
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
