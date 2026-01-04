package com.iam.core.application.service;

import com.iam.core.application.dto.ProvisioningCommand;
import com.iam.core.application.dto.TransformationResult;
import com.iam.core.application.dto.UserSyncEvent;

import com.iam.core.domain.constant.AttributeConstants;
import com.iam.core.domain.constant.SyncConstants;
import com.iam.core.domain.constant.SystemConstants;
import com.iam.core.domain.entity.*;
import com.iam.core.domain.event.SyncCompensationEvent;
import com.iam.core.domain.exception.ErrorCode;
import com.iam.core.domain.exception.IamBusinessException;
import com.iam.core.domain.exception.TransformationException;
import com.iam.core.domain.exception.UserSyncPersistenceException;
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
        Map<String, Object> rawData = event.payload();
        String userName = (String) rawData.get(AttributeConstants.USERNAME);

        // 단계별 결과 보관용 변수
        TransformationResult transResult = null;

        try {
            // 1. [단계: 데이터 변환] Transformation 단계
            transResult = performTransformation(systemId, rawData, traceId);

            // 2. [단계: 동기화 반영] Correlation & Persistence 단계
            performUserSync(transResult, rawData, systemId, traceId);

        } catch (TransformationException te) {
            // 변환 단계 실패: 엔진 오류 혹은 필수 규칙 위반
            handleSyncFailure(traceId, systemId, userName, rawData, te.getMessage(), te.getRevId(), null);
            throw te;
        } catch (UserSyncPersistenceException spe) {
            // 전용 예외에서 iamUserId를 추출하여 이력 기록
            long revId = (transResult != null) ? transResult.revId() : 0L;
            handleSyncFailure(traceId, systemId, userName, rawData, spe.getMessage(), revId, spe.getIamUserId());
            throw spe;
        } catch (IamBusinessException ibe) {
            // 비즈니스 로직 실패: 중복 데이터, 유효성 검사 실패 등
            long revId = (transResult != null) ? transResult.revId() : 0L;
            handleSyncFailure(traceId, systemId, userName, rawData, ibe.getMessage(), revId, null);
            throw ibe;
        } catch (Exception e) {
            // 기타 예상치 못한 런타임 오류
            long revId = (transResult != null) ? transResult.revId() : 0L;
            handleSyncFailure(traceId, systemId, userName, rawData, "Unexpected error: " + e.getMessage(), revId, null);
            throw new IamBusinessException(ErrorCode.MESSAGE_PROCESSING_ERROR, traceId, e.getMessage(), e);
        }
    }

    /**
     * [분리] 변환 로직 처리 및 검증
     */
    private TransformationResult performTransformation(String systemId, Map<String, Object> rawData, String traceId) {
        try {
            var result = transformationService.transform(systemId, rawData);
            validateRequiredAttributes(result.data(), traceId); //
            return result;
        } catch (TransformationException te) {
            log.error("Transformation step failed: {}", te.getMessage());
            throw te;
        } catch (Exception e) {
            throw new TransformationException(e.getMessage(), 0L, e);
        }
    }

    /**
     * [분리] 실제 DB 반영 및 상관관계 처리
     */
    private void performUserSync(TransformationResult result, Map<String, Object> rawData, String systemId, String traceId) {
        String externalId = (String) rawData.get(AttributeConstants.EXTERNAL_ID);
        String userName = (String) result.data().get(AttributeConstants.USERNAME).asString();

        // 1. 먼저 상관관계 분석을 통해 기존 사용자를 찾음
        var optionalUser = correlationService.correlate(systemId, externalId);

        // 2. 실패 시 사용할 iamUserId를 미리 확보 (있을 경우만)
        Long existingUserId = optionalUser.map(IamUser::getId).orElse(null);

        try {
            optionalUser.ifPresentOrElse(
                    user -> updateExistingUser(user, result.data(), rawData, systemId, traceId, rawData, result.revId()),
                    () -> createNewUser(externalId, result.data(), rawData, systemId, traceId, rawData, result.revId()));
        } catch (Exception e) {
            // 수정 실패 시 (existingUserId가 존재할 때) 전용 예외로 던짐
            if (existingUserId != null) {
                throw new UserSyncPersistenceException(ErrorCode.USER_PERSISTENCE_ERROR, traceId, e.getMessage(), existingUserId, e);
            }
            // 신규 생성 실패 시 기존 IamBusinessException 활용
            throw new IamBusinessException(ErrorCode.USER_PERSISTENCE_ERROR, traceId, e.getMessage(), e);
        }
    }

    /**
     * [공통] 실패 이력 기록 및 보상 트랜잭션 발행
     */
    private void handleSyncFailure(String traceId, String systemId, String userName, Map<String, Object> payload, String errorMsg, long revId, Long iamUserId) {
        syncHistoryService.logFailure(traceId, SyncConstants.EVENT_SYNC_ERROR,
                userName != null ? userName : "UNKNOWN", iamUserId, systemId, SystemConstants.SYSTEM_IAM,
                payload, errorMsg, revId);

        // 보상 이벤트 발행 로직
        sendCompensationEvent(traceId, systemId, payload, errorMsg);
    }

    /**
     *  보상 이벤트 발행 로직
     */
    private void sendCompensationEvent(String traceId, String systemId, Map<String, Object> payload, String errorMsg) {
        String externalId = (String) payload.get(AttributeConstants.EXTERNAL_ID);
        if (externalId != null && !externalId.isBlank()) {
            // 이벤트 객체 생성 (당시의 시간과 에러 메시지 포함)
            SyncCompensationEvent compEvent =
                    new SyncCompensationEvent(
                            traceId,
                            systemId,
                            externalId,
                            errorMsg,
                            java.time.LocalDateTime.now()
                    );

            // 전용 Exchange와 Routing Key를 사용하여 메시지 발행
            messagePublisher.publish(EXCHANGE_NAME, COMPENSATION_ROUTING_KEY, compEvent);

            log.info("Published compensation event - traceId: {}, system: {}, externalId: {}, reason: {}", traceId, systemId, externalId, errorMsg);
        } else {
            log.warn("Skipped compensation event due to missing externalId - traceId: {}", traceId);
        }
    }

    private void createNewUser(String externalId, Map<String, UniversalData> attributes,
                               Map<String, Object> rawData, String systemId, String traceId, Object originalRequest,
                               long appliedRules) {
        log.info("Creating new user via engine for system: {}, ID: {}", systemId, externalId);

        var user = iamUserUpdateService.create(externalId, attributes);
        var link = createIdentityLink(user.getId(), systemId, externalId);
        identityLinkRepository.save(link);

        publishProvisioningCommand(user, extractRawExtensions(attributes), traceId);

        // Normalized History: No more huge mappings nested data
        Map<String, Object> resultSnapshot = extractRawExtensions(attributes);

        syncHistoryService.logSuccess(traceId, SyncConstants.EVENT_USER_CREATE, user.getUserName(), user.getId(),
                systemId,
                SystemConstants.SYSTEM_IAM,
                resultSnapshot,
                "User created via engine", null, null, rawData, appliedRules);
    }

    private void updateExistingUser(IamUser user, Map<String, UniversalData> attributes,
                                    Map<String, Object> rawData, String systemId, String traceId, Object originalRequest,
                                    long appliedRules) {
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
                SystemConstants.SYSTEM_IAM,
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
                SystemConstants.SYSTEM_AD,
                cmdMap,
                "Provisioning request sent to AD",
                null,
                null,
                null,
                null);
    }
}
