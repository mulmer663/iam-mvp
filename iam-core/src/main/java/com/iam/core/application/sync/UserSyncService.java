package com.iam.core.application.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.core.application.common.ProvisioningCommand;
import com.iam.core.application.common.TransformationResult;
import com.iam.core.application.common.UserSyncEvent;
import com.iam.core.application.user.IamUserUpdateService;
import com.iam.core.application.user.IdentityCorrelationService;
import com.iam.core.domain.common.constant.AttributeConstants;
import com.iam.core.domain.common.constant.SyncConstants;
import com.iam.core.domain.common.constant.SystemConstants;
import com.iam.core.domain.common.event.SyncCompensationEvent;
import com.iam.core.domain.common.exception.ErrorCode;
import com.iam.core.domain.common.exception.IamBusinessException;
import com.iam.core.domain.common.exception.TransformationException;
import com.iam.core.domain.common.exception.UserSyncPersistenceException;
import com.iam.core.domain.common.port.MessagePublisher;
import com.iam.core.domain.common.vo.UniversalData;
import com.iam.core.domain.user.IamUser;
import com.iam.core.domain.user.IdentityLink;
import com.iam.core.domain.user.IdentityLinkRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

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

            // 2. 사용자 정보 반영 (Core Logic) 및 후처리에 필요한 컨텍스트 반환
            SyncContext context = performUserSync(transResult, rawData, systemId, traceId);

            // 3. 공통 후처리 (Side Effects: Provisioning & Logging)
            postProcessSync(context, transResult, event);

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
        } catch (TransformationException | IamBusinessException te) {
            log.error("Transformation step failed: {}", te.getMessage());
            throw te;
        } catch (Exception e) {
            throw new TransformationException(e.getMessage(), 0L, e);
        }
    }

    /**
     * [분리] 실제 DB 반영 및 상관관계 처리
     */
    private SyncContext performUserSync(TransformationResult result, Map<String, Object> rawData, String systemId,
            String traceId) {
        String externalId = (String) rawData.get(AttributeConstants.EXTERNAL_ID);
        var optionalUser = correlationService.correlate(systemId, externalId);
        Long existingUserId = optionalUser.map(IamUser::getId).orElse(null);

        try {
            return optionalUser.map(user -> handleUpdate(user, result.data()))
                    .orElseGet(() -> handleCreate(externalId, result.data(), systemId));
        } catch (Exception e) {
            if (existingUserId != null) {
                throw new UserSyncPersistenceException(ErrorCode.USER_PERSISTENCE_ERROR, traceId, e.getMessage(),
                        existingUserId, e);
            }
            throw new IamBusinessException(ErrorCode.USER_PERSISTENCE_ERROR, traceId, e.getMessage(), e);
        }
    }

    private void postProcessSync(SyncContext context, TransformationResult transResult, UserSyncEvent event) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        long userRevId;
        try {
            // 해당 사용자의 리비전 목록 중 마지막 번호 추출
            Number revisionNumber = reader.getRevisionNumberForDate(new Date());
            userRevId = revisionNumber.longValue();
        } catch (Exception e) {
            log.warn("Failed to fetch user revision, defaulting to 0. TraceId: {}", event.traceId());
            userRevId = 0L;
        }

        // 1. 외부 시스템 프로비저닝 명령 발행
        publishProvisioningCommand(context.user(), extractRawExtensions(transResult.data()), event.traceId(),
                userRevId);

        // 2. 통합 이력 저장 (Normalized)
        syncHistoryService.logSuccess(
                event.traceId(),
                SyncConstants.DIRECTION_RECON,
                context.eventType(),
                context.user().getUserName(),
                context.user().getId(),
                event.systemId(),
                SystemConstants.SYSTEM_IAM,
                context.resultSnapshot(),
                context.logMessage(),
                null,
                Map.of("event", event),
                userRevId,
                transResult.revId());
    }

    /**
     * [공통] 실패 이력 기록 및 보상 트랜잭션 발행
     */
    private void handleSyncFailure(String traceId, String systemId, String userName, Map<String, Object> payload,
            String errorMsg, long ruleRevId, Long iamUserId) {
        syncHistoryService.logFailure(traceId, SyncConstants.DIRECTION_RECON, SyncConstants.EVENT_SYNC_ERROR,
                userName != null ? userName : "UNKNOWN", iamUserId, systemId, SystemConstants.SYSTEM_IAM,
                payload, errorMsg, ruleRevId);

        // 보상 이벤트 발행 로직
        sendCompensationEvent(traceId, systemId, payload, errorMsg);
    }

    private SyncContext handleCreate(String externalId, Map<String, UniversalData> attributes, String systemId) {
        log.info("Creating user: {}", externalId);
        var user = iamUserUpdateService.create(externalId, attributes);
        identityLinkRepository.save(createIdentityLink(user.getId(), systemId, externalId));

        return new SyncContext(user, SyncConstants.EVENT_USER_CREATE, createUserResultData(user),
                "User created via engine");
    }

    private Map<String, Object> createUserResultData(IamUser user) {
        Map<String, Object> result = new HashMap<>();
        result.put(AttributeConstants.SYNC_TYPE, SyncConstants.EVENT_USER_CREATE);
        result.put("status", "CREATED");
        // 상세 정보는 revId를 통해 AUD에서 조회하므로 최소한의 메타데이터만 남김
        return result;
    }

    private SyncContext handleUpdate(IamUser user, Map<String, UniversalData> attributes) {
        log.info("Updating user: {}", user.getId());
        Map<String, Object> oldState = captureState(user);
        iamUserUpdateService.update(user, attributes);

        Map<String, Object> resultData = modifyUserResultData(user, oldState);
        String eventType = (String) resultData.get(AttributeConstants.SYNC_TYPE);

        return new SyncContext(user, eventType, resultData, "User updated via engine");
    }

    private Map<String, Object> modifyUserResultData(IamUser user, Map<String, Object> oldState) {
        Map<String, Object> newState = captureState(user);
        List<Map<String, Object>> changes = calculateChanges(oldState, newState);

        // TODO: 동적 항목 중요도 설정(Attribute Importance) 기능 구현 필요
        // 현재는 '부서(department)' 필드 포함 여부로 CRITICAL 여부를 하드코딩함
        boolean hasCriticalChange = changes.stream()
                .anyMatch(
                        change -> ((String) change.get(AttributeConstants.FIELD)).toLowerCase().contains("department"));

        Map<String, Object> result = new HashMap<>();
        result.put(AttributeConstants.CHANGES, changes);
        result.put(AttributeConstants.SYNC_TYPE,
                hasCriticalChange ? SyncConstants.EVENT_USER_UPDATE_CRITICAL : SyncConstants.EVENT_USER_UPDATE_SIMPLE);
        return result;
    }

    /**
     * 후처리에 필요한 데이터를 전달하기 위한 내부 Record
     */
    private record SyncContext(IamUser user, String eventType, Map<String, Object> resultSnapshot, String logMessage) {

    }

    /**
     * 보상 이벤트 발행 로직
     */
    private void sendCompensationEvent(String traceId, String systemId, Map<String, Object> payload, String errorMsg) {
        String externalId = (String) payload.get(AttributeConstants.EXTERNAL_ID);
        if (externalId != null && !externalId.isBlank()) {
            // 이벤트 객체 생성 (당시의 시간과 에러 메시지 포함)
            SyncCompensationEvent compEvent = new SyncCompensationEvent(
                    traceId,
                    systemId,
                    externalId,
                    errorMsg,
                    java.time.LocalDateTime.now());

            // 전용 Exchange와 Routing Key를 사용하여 메시지 발행
            messagePublisher.publish(EXCHANGE_NAME, COMPENSATION_ROUTING_KEY, compEvent);

            log.info("Published compensation event - traceId: {}, system: {}, externalId: {}, reason: {}", traceId,
                    systemId, externalId, errorMsg);
        } else {
            log.warn("Skipped compensation event due to missing externalId - traceId: {}", traceId);
        }
    }

    private Map<String, Object> captureState(IamUser user) {
        if (user == null)
            return Collections.emptyMap();

        Map<String, Object> state = new HashMap<>();

        // 1. IamUser 기본 속성 추출
        state.put(AttributeConstants.USERNAME, user.getUserName());
        state.put(AttributeConstants.FAMILY_NAME, user.getFamilyName());
        state.put(AttributeConstants.GIVEN_NAME, user.getGivenName());
        state.put(AttributeConstants.TITLE, user.getTitle());
        state.put(AttributeConstants.ACTIVE, user.isActive());
        state.put(AttributeConstants.EXTERNAL_ID, user.getExternalId());

        // 2. IamUserExtension 내의 다형성 확장 데이터 추출
        if (user.getExtension() != null && user.getExtension().getExtensions() != null) {
            user.getExtension().getExtensions().forEach((schemaUri, extensionData) -> {
                // ExtensionData(예: EnterpriseUserExtension)를 Map으로 변환하여 평면화
                try {
                    // ObjectMapper를 사용하여 객체의 필드들을 Map으로 변환
                    @SuppressWarnings("unchecked")
                    Map<String, Object> attrMap = objectMapper.convertValue(extensionData, Map.class);

                    if (attrMap != null) {
                        attrMap.forEach((attrKey, attrValue) -> {
                            // "urn:uri:attrName" 형식의 키 생성
                            state.put(schemaUri + ":" + attrKey, attrValue);
                        });
                    }
                } catch (Exception e) {
                    log.warn("Failed to capture extension state for schema: {}", schemaUri);
                }
            });
        }

        return state;
    }

    private List<Map<String, Object>> calculateChanges(Map<String, Object> oldState, Map<String, Object> newState) {
        List<Map<String, Object>> changes = new ArrayList<>();

        newState.forEach((key, newValue) -> {
            Object oldValue = oldState.get(key);

            // 두 값이 모두 null이거나 같으면 무시
            if (Objects.equals(oldValue, newValue))
                return;

            // 문자열 변환 및 비교 (null은 빈 문자열로 처리하거나 "null"로 명시)
            String oldStr = (oldValue == null) ? "null" : String.valueOf(oldValue);
            String newStr = (newValue == null) ? "null" : String.valueOf(newValue);

            if (!oldStr.equals(newStr)) {
                changes.add(Map.of(
                        AttributeConstants.FIELD, key,
                        AttributeConstants.OLD_VALUE, oldStr,
                        AttributeConstants.NEW_VALUE, newStr));
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

    private void publishProvisioningCommand(IamUser user, Map<String, Object> attributes, String traceId,
            long userRevId) {
        // 1. AD 규격에 맞는 Command 생성 (이것이 IAM이 보낸 최종 Payload)
        var adPayload = new ProvisioningCommand.AdProvisioningPayload(
                user.getUserName(), // sAMAccountName
                user.getUserName() + "@global-iam.com", // userPrincipalName (예시)
                null, // DN (커넥터에서 생성하거나 규칙에 따름)
                user.getFamilyName(), // sn
                user.getGivenName(), // givenName
                user.getFormattedName(), // displayName
                user.getTitle(), // title
                (String) attributes.get("email"), // mail
                user.isActive() // enabled
        );

        var command = new ProvisioningCommand(traceId, "CAUSE_EVENT_ID", "CREATE", adPayload);

        // 2. 메시지 발행
        messagePublisher.publish(EXCHANGE_NAME, PROVISION_ROUTING_KEY, command);
        log.info("Published provisioning command for user: {} (traceId: {})", user.getId(), traceId);

        // 3. AD 연동 이력 기록 (SyncHistory 활용)
        syncHistoryService.logSuccess(
                traceId,
                SyncConstants.DIRECTION_PROV,
                SyncConstants.EVENT_USER_CREATE,
                user.getUserName(),
                user.getId(),
                SystemConstants.SYSTEM_IAM, // Source: IAM
                SystemConstants.SYSTEM_AD, // Target: AD
                Map.of("status", "SENT", "target", "AD"), // resultData: 일단 전송 상태 기록
                "Provisioning request sent to AD",
                null,
                Map.of("command", command), // requestPayload: IAM이 보낸 전문 통째로 저장
                userRevId, // 이 명령의 근거가 된 유저 리비전
                0L // 규칙 리비전 (필요 시 할당)
        );
    }
}
