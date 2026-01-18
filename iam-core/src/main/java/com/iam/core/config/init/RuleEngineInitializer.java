package com.iam.core.config.init;

import com.iam.core.application.service.TransMappingService;
import com.iam.core.domain.constant.SystemConstants;
import com.iam.core.domain.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 규칙 엔진 초기 데이터 이니셜라이저.
 * 변환 규칙(TransRule), 코드 매핑, 필드 매핑 등을 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
@Profile({ "local", "dev", "test", "default" })
public class RuleEngineInitializer implements CommandLineRunner {

    private final TransMappingService transMappingService;

    @Override
    public void run(String... args) {
        initRuleEngineData();
    }

    private void initRuleEngineData() {
        if (transMappingService.countRuleMeta() > 0)
            return;

        log.info("🚀 [RuleEngineInitializer] 규칙 엔진 초기 데이터 생성을 시작합니다...");

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

        log.info("✅ [RuleEngineInitializer] {} 규칙 매핑을 생성했습니다.", SystemConstants.SYSTEM_SAP_HR);
    }

    private void createCodeMappings() {
        String groupId = "RANK_CODE";
        transMappingService.saveCodeMeta(TransCodeMeta.builder()
                .codeGroupId(groupId)
                .description("Rank Code Mapping (HR -> IAM)")
                .build());

        transMappingService.saveCodeValues(List.of(
                TransCodeValue.builder().codeGroupId(groupId).sourceValue("A").targetValue("1")
                        .label("사원").build(),
                TransCodeValue.builder().codeGroupId(groupId).sourceValue("B").targetValue("2")
                        .label("대리").build(),
                TransCodeValue.builder().codeGroupId(groupId).sourceValue("C").targetValue("3")
                        .label("과장").build()));
    }

    private void createFieldMappings(String ruleId) {
        List<TransFieldMapping> mappings = List.of(
                // 1. Core Attributes (IamUser fields)
                TransFieldMapping.builder().ruleId(ruleId).sourceField("email").targetField("userName")
                        .isRequired(true)
                        .build(),
                TransFieldMapping.builder().ruleId(ruleId).sourceField("lastName")
                        .targetField("familyName").build(),
                TransFieldMapping.builder().ruleId(ruleId).sourceField("firstName")
                        .targetField("givenName").build(),
                TransFieldMapping.builder().ruleId(ruleId).sourceField("position").targetField("title")
                        .build(),

                // 2. Active Status (Custom logic to handle "ACTIVE" -> true)
                TransFieldMapping.builder().ruleId(ruleId).sourceField("status").targetField("active")
                        .transformType("CUSTOM")
                        .transformScript(
                                "new com.iam.core.domain.vo.BooleanData(source.status?.asString() == 'ACTIVE')")
                        .build(),

                // 3. Extension Attributes (EnterpriseUserExtension)
                TransFieldMapping.builder().ruleId(ruleId).sourceField("empNo")
                        .targetField("employeeNumber")
                        .isRequired(true)
                        .build(),
                TransFieldMapping.builder().ruleId(ruleId).sourceField("deptCode")
                        .targetField("department").build());

        transMappingService.saveMappings(ruleId, mappings);
    }
}
