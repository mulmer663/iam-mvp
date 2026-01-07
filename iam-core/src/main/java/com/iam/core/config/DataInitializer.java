package com.iam.core.config;

import com.iam.core.application.dto.UserSyncEvent;
import com.iam.core.application.service.IamUserUpdateService;
import com.iam.core.application.service.SyncHistoryService;
import com.iam.core.application.service.TransMappingService;
import com.iam.core.domain.constant.AttributeConstants;
import com.iam.core.domain.constant.SystemConstants;
import com.iam.core.domain.entity.*;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.vo.StringData;
import com.iam.core.domain.vo.UniversalData;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.iam.core.domain.constant.SyncConstants.*;

/**
 * 로컬 개발 환경용 초기 데이터 이니셜라이저.
 * iam-ui의 mocks/data.ts와 동일한 데이터를 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile({ "local", "dev", "test", "default" }) // 운영 환경에서는 실행되지 않도록 제한
public class DataInitializer implements CommandLineRunner {

    private final IamUserRepository iamUserRepository;
    private final IamUserUpdateService iamUserUpdateService;
    private final SyncHistoryService syncHistoryService;
    private final TransMappingService transMappingService;

    @Override
    public void run(String... args) {
        initRuleEngineData();

        String traceId = "T-" + TSID.fast().toLong();
        MDC.put(TRACE_ID, traceId);
        MDC.put(OPERATION_TYPE, EVENT_USER_CREATE);

        try {
            if (iamUserRepository.count() > 0) {
                log.info("ℹ️ DB 데이터 존재로 초기화 건너뜜");
                return;
            }

            log.info("🚀 샘플 데이터 생성 시작...");

            // 2. 각 서비스 호출 시점에 트랜잭션이 수행되도록 함
            IamUser admin = createUserViaService("super.admin", "Michael", "Admin", "IT Director", true,
                    "michael.admin@global-iam.com", "GLOBAL-IT", "ADM001");
            IamUser jane = createUserViaService("jane.doe", "Jane", "Doe", "External Auditor", true,
                    "jane.doe@audit-firm.com", "AUDIT-01", "EXT-101");

            log.info("✅ 사용자 데이터 생성 완료");

            // 3. 이력 생성
            createSyncHistoryViaService(jane, traceId);

        } finally {
            MDC.clear(); // 반드시 마지막에 clear
        }
    }

    private IamUser createUserViaService(String userName, String given, String family, String title, boolean active,
            String email, String dept, String empNo) {
        // 맵 구조로 attributes 생성 (Service 입력 규격에 맞춤)
        Map<String, UniversalData> attributes = new HashMap<>();
        attributes.put("userName", new StringData(userName));
        attributes.put("givenName", new StringData(given));
        attributes.put("familyName", new StringData(family));
        attributes.put("title", new StringData(title));
        attributes.put("active", new com.iam.core.domain.vo.BooleanData(active));
        attributes.put("email", new StringData(email));
        attributes.put("department", new StringData(dept));
        attributes.put("employeeNumber", new StringData(empNo));

        return iamUserUpdateService.create(empNo, attributes);
    }

    private void initRuleEngineData() {
        if (transMappingService.countRuleMeta() > 0)
            return;

        log.info("🚀 [DataInitializer] 규칙 엔진 초기 데이터 생성을 시작합니다...");

        String ruleId = "SAP_CORE_TRANS";
        transMappingService.saveRuleMeta(TransRuleMeta.builder()
                .ruleId(ruleId)
                .ruleName("SAP HR Core Transformation")
                .targetAttribute("CORE")
                .status("ACTIVE")
                .build());

        transMappingService.saveTransMapping(TransMapping.builder()
                .systemId(SystemConstants.SYSTEM_SAP_HR)
                .ruleId(ruleId)
                .execOrder(1)
                .isMandatory(true)
                .build());

        // Initial Field Mappings
        createCodeMappings();
        createFieldMappings(ruleId);

        log.info("✅ [DataInitializer] {} 규칙 매핑을 생성했습니다.", SystemConstants.SYSTEM_SAP_HR);
    }

    private void createCodeMappings() {
        String groupId = "RANK_CODE";
        transMappingService.saveCodeMeta(TransCodeMeta.builder()
                .codeGroupId(groupId)
                .description("Rank Code Mapping (HR -> IAM)")
                .build());

        transMappingService.saveCodeValues(List.of(
                TransCodeValue.builder().codeGroupId(groupId).sourceValue("A").targetValue("1").label("사원").build(),
                TransCodeValue.builder().codeGroupId(groupId).sourceValue("B").targetValue("2").label("대리").build(),
                TransCodeValue.builder().codeGroupId(groupId).sourceValue("C").targetValue("3").label("과장").build()));
    }

    private void createFieldMappings(String ruleId) {
        List<TransFieldMapping> mappings = List.of(
                // 1. Core Attributes (IamUser fields)
                TransFieldMapping.builder().ruleId(ruleId).sourceField("email").targetField("userName").isRequired(true)
                        .build(),
                TransFieldMapping.builder().ruleId(ruleId).sourceField("lastName").targetField("familyName").build(),
                TransFieldMapping.builder().ruleId(ruleId).sourceField("firstName").targetField("givenName").build(),
                TransFieldMapping.builder().ruleId(ruleId).sourceField("position").targetField("title").build(),

                // 2. Active Status (Custom logic to handle "ACTIVE" -> true)
                TransFieldMapping.builder().ruleId(ruleId).sourceField("status").targetField("active")
                        .transformType("CUSTOM")
                        .transformScript(
                                "new com.iam.core.domain.vo.BooleanData(source.status?.asString() == 'ACTIVE')")
                        .build(),

                // 3. Extension Attributes (EnterpriseUserExtension)
                TransFieldMapping.builder().ruleId(ruleId).sourceField("empNo").targetField("employeeNumber")
                        .isRequired(true)
                        .build(),
                TransFieldMapping.builder().ruleId(ruleId).sourceField("deptCode").targetField("department").build());

        transMappingService.saveMappings(ruleId, mappings);
    }

    private void createSyncHistoryViaService(IamUser user, String traceId) {
        String systemId = "SAP_HR";

        // 1. 실제 런타임과 동일한 UserSyncEvent 전문(Full Object) 생성
        Map<String, Object> payload = new HashMap<>();
        payload.put("empNo", "EXT-101");
        payload.put("email", user.getUserName());
        payload.put("status", "ACTIVE");
        payload.put("firstName", user.getGivenName());
        payload.put("lastName", user.getFamilyName());

        UserSyncEvent mockEvent = new UserSyncEvent(
                traceId,
                systemId,
                EVENT_USER_CREATE, // "USER_CREATE"
                java.time.LocalDateTime.now(), // timestamp
                payload // payload
        );

        // 2. 결과 데이터 시뮬레이션 (전체 스냅샷 대신 최소 메타데이터만)
        Map<String, Object> resultSnapshot = new HashMap<>();
        resultSnapshot.put(AttributeConstants.SYNC_TYPE, EVENT_USER_CREATE);
        resultSnapshot.put("status", "CREATED");

        // 3. Service 호출하여 이력 기록 (수정된 파라미터 반영)
        syncHistoryService.logSuccess(
                traceId,
                DIRECTION_RECON,
                EVENT_USER_CREATE,
                user.getUserName(),
                user.getId(),
                systemId,
                SystemConstants.SYSTEM_IAM,
                resultSnapshot,
                "User created via DataInitializer",
                null, // parentId
                Map.of("event", mockEvent), // requestPayload: Event 객체를 통째로 저장
                1L, // userRevId: 가상의 사용자 리비전
                1L // ruleRevId: 가상의 규칙 리비전
        );
    }
}