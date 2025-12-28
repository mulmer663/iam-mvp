package com.iam.core.application.service;

import com.iam.core.application.dto.ProvisioningCommand;
import com.iam.core.application.dto.UserSyncEvent;
import com.iam.core.application.dto.UserSyncPayload;
import com.iam.core.domain.entity.*;
import com.iam.core.domain.exception.ErrorCode;
import com.iam.core.domain.exception.IamBusinessException;
import com.iam.core.domain.port.MessagePublisher;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.IdentityLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    private final IamUserRepository iamUserRepository;
    private final IdentityLinkRepository identityLinkRepository;
    private final MessagePublisher messagePublisher;
    private final SyncHistoryService syncHistoryService;
    private final TransformationService transformationService;
    private final IamUserUpdateService iamUserUpdateService;
    private final UniversalMapper universalMapper;

    private static final String PROVISION_ROUTING_KEY = "cmd.ad.user.create";
    private static final String EXCHANGE_NAME = "iam.topic";
    private static final String ENTERPRISE_URN = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

    /**
     * HR 시스템으로부터 수신한 사용자 동기화 이벤트를 처리
     */
    @Transactional
    public void processHrSync(UserSyncEvent event) {
        String traceId = event.traceId();
        log.info("Processing HR sync migration for traceId: {}", traceId);

        var payload = event.payload();
        Map<String, Object> rawData = convertToMap(payload);

        try {
            // 1. Transform Attribute using Engine
            Map<String, com.iam.core.domain.vo.UniversalData> transformed = transformationService.transform("SAP_HR",
                    rawData);

            // 2. Correlation & Update
            identityLinkRepository.findBySystemTypeAndExternalId("HR", payload.getExternalId())
                    .ifPresentOrElse(
                            link -> updateExistingUser(link, transformed, rawData, traceId),
                            () -> createNewUser(payload.getExternalId(), transformed, rawData, traceId));

        } catch (Exception e) {
            log.error("Sync failed for traceId: {}", traceId, e);
            syncHistoryService.logFailure(traceId, "HR_SYNC", payload.getUserName(), "SAP_HR", payload,
                    "Error: " + e.getMessage());

            if (e instanceof IamBusinessException) {
                throw e;
            }
            throw new IamBusinessException(ErrorCode.MESSAGE_PROCESSING_ERROR, traceId, e.getMessage(), e);
        }
    }

    private Map<String, Object> convertToMap(UserSyncPayload payload) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("externalId", payload.getExternalId());
        map.put("userName", payload.getUserName());
        if (payload.getName() != null) {
            map.put("familyName", payload.getName().getFamilyName());
            map.put("givenName", payload.getName().getGivenName());
            map.put("formattedName", payload.getName().getFormatted());
        }
        map.put("title", payload.getTitle());
        map.put("active", payload.getActive());
        if (payload.getExtensions() != null) {
            map.putAll(payload.getExtensions());
        }
        return map;
    }

    private void createNewUser(String externalId, Map<String, com.iam.core.domain.vo.UniversalData> attributes,
            Map<String, Object> rawData, String traceId) {
        log.info("Creating new user via engine for HR ID: {}", externalId);

        var user = iamUserUpdateService.create(externalId, attributes);
        var link = createIdentityLink(user.getId(), externalId);
        identityLinkRepository.save(link);

        publishProvisioningCommand(user, extractRawExtensions(attributes));

        // Rich History
        Map<String, Object> historyPayload = Map.of(
                "syncType", "JOIN",
                "mappings", generateMappings("HR", attributes),
                "snapshot", Map.of(
                        "layer", "HR",
                        "data", rawData));

        syncHistoryService.logSuccess(traceId, "HR_SYNC", user.getUserName(), "SAP_HR", historyPayload,
                "User created via engine");
    }

    private void updateExistingUser(IdentityLink link, Map<String, com.iam.core.domain.vo.UniversalData> attributes,
            Map<String, Object> rawData, String traceId) {
        log.info("Updating existing user via engine for HR ID: {}", link.getExternalId());

        iamUserRepository.findById(link.getIamUserId()).ifPresentOrElse(
                user -> {
                    // Capture old state for diff
                    Map<String, Object> oldState = captureState(user);

                    iamUserUpdateService.update(user, attributes);
                    publishProvisioningCommand(user, extractRawExtensions(attributes));

                    // Calculate changes
                    List<Map<String, Object>> changes = calculateChanges(oldState, captureState(user));

                    // Rich History
                    Map<String, Object> historyPayload = new HashMap<String, Object>();
                    historyPayload.put("syncType", changes.isEmpty() ? "UPDATE_SIMPLE" : "UPDATE_CRITICAL");
                    historyPayload.put("changes", changes);
                    historyPayload.put("mappings", generateMappings("HR", attributes));
                    historyPayload.put("snapshot", Map.of("layer", "HR", "data", rawData));

                    syncHistoryService.logSuccess(traceId, "USER_UPDATE", user.getUserName(), "SAP_HR",
                            historyPayload, "User updated via engine");
                },
                () -> {
                    throw new IamBusinessException(ErrorCode.USER_NOT_FOUND, traceId,
                            "User not found: " + link.getIamUserId());
                });
    }

    private Map<String, Object> captureState(IamUser user) {
        Map<String, Object> state = new HashMap<String, Object>();
        state.put("userName", user.getUserName());
        state.put("familyName", user.getFamilyName());
        state.put("givenName", user.getGivenName());
        state.put("title", user.getTitle());
        state.put("active", user.isActive());
        // Extensions could be added here if needed
        return state;
    }

    private List<Map<String, Object>> calculateChanges(Map<String, Object> oldState, Map<String, Object> newState) {
        List<Map<String, Object>> changes = new ArrayList<>();
        newState.forEach((k, v) -> {
            Object oldVal = oldState.get(k);
            if (v != null && !v.equals(oldVal)) {
                changes.add(Map.of("field", k, "old", String.valueOf(oldVal), "new", String.valueOf(v)));
            }
        });
        return changes;
    }

    private List<Map<String, Object>> generateMappings(String from,
            Map<String, com.iam.core.domain.vo.UniversalData> attributes) {
        List<Map<String, Object>> mappings = new ArrayList<>();
        attributes.forEach((k, v) -> {
            mappings.add(Map.of(
                    "fromLabel", from,
                    "toLabel", "IAM",
                    "fromField", k, // For now assuming 1:1 name for simplicity if not traceable
                    "toField", k,
                    "value", v.asString()));
        });
        return mappings;
    }

    private Map<String, Object> extractRawExtensions(Map<String, com.iam.core.domain.vo.UniversalData> attributes) {
        Map<String, Object> raw = new HashMap<String, Object>();
        attributes.forEach((k, v) -> raw.put(k, v.getValue()));
        return raw;
    }

    private IdentityLink createIdentityLink(Long iamUserId, String externalId) {
        var link = new IdentityLink();
        link.setIamUserId(iamUserId);
        link.setSystemType("HR");
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
