package com.iam.core.application.service;

import com.iam.core.application.dto.UserSyncEvent;
import com.iam.core.domain.constant.AttributeConstants;
import com.iam.core.domain.constant.SyncConstants;
import com.iam.core.domain.entity.*;
import com.iam.core.domain.enums.AttributeCategory;
import com.iam.core.domain.enums.AttributeDataType;
import com.iam.core.domain.enums.AttributeTargetDomain;
import com.iam.core.domain.port.MessagePublisher;
import com.iam.core.domain.repository.IamAttributeMetaRepository;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.IdentityLinkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserSyncService 단위 테스트
 * MQ와 무관하게 순수 비즈니스 로직만 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserSyncServiceTest {

        @Autowired
        private UserSyncService userSyncService;

        @MockitoBean
        private MessagePublisher messagePublisher;

        @MockitoBean
        private SyncHistoryService syncHistoryService;

        @Autowired
        private IamUserRepository iamUserRepository;

        @Autowired
        private IamAttributeMetaRepository attributeMetaRepository;

        @Test
        @DisplayName("Custom Attribute 매핑 시 GenericExtension에 동적으로 저장되어야 한다")
        void processHrSync_CustomAttribute_ShouldSaveToGenericExtension() {
                // Given
                String hrEmpId = "H003";
                String customAttr = "customBadge";

                // 1. Define Custom Attribute Meta
                attributeMetaRepository.save(IamAttributeMeta.builder()
                                .name(customAttr)
                                .targetDomain(AttributeTargetDomain.USER)
                                .category(AttributeCategory.CUSTOM) // CUSTOM category
                                .displayName("Custom Badge")
                                .type(AttributeDataType.STRING)
                                .build());

                // 2. Add Mapping (badge -> customBadge)
                transMappingService.saveMapping(TransFieldMapping.builder()
                                .ruleId(RULE_ID)
                                .sourceField("badge")
                                .targetField(customAttr)
                                .build());

                // 3. Sync Payload
                Map<String, Object> payload = Map.of(
                                "externalId", hrEmpId,
                                "email", "dynamic.test@iam.com",
                                "lastName", "Dynamic",
                                "firstName", "Test",
                                "badge", "BADGE-123456", // Source field
                                "empNo", hrEmpId // Mandatory
                );

                UserSyncEvent event = new UserSyncEvent("trace-dynamic-1", SYSTEM_ID, SyncConstants.EVENT_USER_CREATE,
                                LocalDateTime.now(), payload);

                // When
                userSyncService.processSync(event);

                // Then
                var link = identityLinkRepository.findBySystemTypeAndExternalId(SYSTEM_ID, hrEmpId).get();
                var user = iamUserRepository.findById(link.getIamUserId()).get();

                // Verify Core
                assertThat(user.getUserName()).isEqualTo("dynamic.test@iam.com");

                // Verify Generic Extension
                var extData = user.getExtension().getExtensions().get("GenericExtension");
                assertThat(extData).isNotNull();
                assertThat(extData).isInstanceOf(com.iam.core.domain.entity.GenericExtension.class);

                Map<String, Object> attrs = ((com.iam.core.domain.entity.ExtensionData) extData).getAttributes();
                assertThat(attrs).containsEntry(customAttr, "BADGE-123456");
        }

        @Autowired
        private IdentityLinkRepository identityLinkRepository;

        @Autowired
        private TransMappingService transMappingService;

        private static final String SYSTEM_ID = "TEST_HR";
        private static final String RULE_ID = "TEST_RULE";

        @org.junit.jupiter.api.BeforeEach
        void setup() {
                // 1. Rule Meta
                if (transMappingService.getMappings(RULE_ID).isEmpty()) {
                        transMappingService.saveRuleMeta(TransRuleMeta.builder()
                                        .ruleId(RULE_ID)
                                        .ruleName("Test Rule")
                                        .targetAttribute("CORE")
                                        .status("ACTIVE")
                                        .build());

                        // 2. Trans Mapping (System -> Rule)
                        transMappingService.saveTransMapping(TransMapping.builder()
                                        .systemId(SYSTEM_ID)
                                        .ruleId(RULE_ID)
                                        .execOrder(1)
                                        .isMandatory(true)
                                        .build());

                        // 3. Field Mappings
                        createTestFieldMappings();
                }
        }

        private void createTestFieldMappings() {
                // Map raw keys (from test payload) to IAM AttributeConstants
                java.util.List<TransFieldMapping> mappings = java.util.List.of(
                                TransFieldMapping.builder().ruleId(RULE_ID)
                                                .sourceField("email")
                                                .targetField(AttributeConstants.USERNAME).isRequired(true).build(),
                                TransFieldMapping.builder().ruleId(RULE_ID)
                                                .sourceField("lastName")
                                                .targetField(AttributeConstants.FAMILY_NAME).build(),
                                TransFieldMapping.builder().ruleId(RULE_ID)
                                                .sourceField("firstName")
                                                .targetField(AttributeConstants.GIVEN_NAME).build(),
                                TransFieldMapping.builder().ruleId(RULE_ID)
                                                .sourceField("title")
                                                .targetField(AttributeConstants.TITLE).build(),
                                TransFieldMapping.builder().ruleId(RULE_ID)
                                                .sourceField("active")
                                                .targetField(AttributeConstants.ACTIVE).build(),
                                // Extensions
                                TransFieldMapping.builder().ruleId(RULE_ID)
                                                .sourceField("empNo")
                                                .targetField("employeeNumber").isRequired(true).build(),
                                TransFieldMapping.builder().ruleId(RULE_ID)
                                                .sourceField("deptName")
                                                .targetField("department").build());

                mappings.forEach(transMappingService::saveMapping);
        }

        @Test
        @DisplayName("HR 신규 사용자 동기화 시 DB에 정상 저장되고 컬럼과 JSONB가 분리되어야 한다")
        void processHrSync_NewUser_ShouldSaveHybrid() {
                // Given
                String hrEmpId = "H001";
                Map<String, Object> payload = Map.of(
                                "externalId", hrEmpId,
                                "email", "hong.g@iam.com",
                                "lastName", "Hong",
                                "firstName", "Gildong",
                                "title", "Senior Engineer",
                                "active", true,
                                "empNo", hrEmpId,
                                "deptName", "Dev Team");

                UserSyncEvent event = new UserSyncEvent("trace-123", SYSTEM_ID, SyncConstants.EVENT_USER_CREATE,
                                LocalDateTime.now(),
                                payload);

                // When
                userSyncService.processSync(event);

                // Then
                var link = identityLinkRepository.findBySystemTypeAndExternalId(SYSTEM_ID, hrEmpId).get();
                Long userId = link.getIamUserId();
                assertThat(userId).isNotNull();
                var user = iamUserRepository.findById(userId).get();

                assertThat(user.getUserName()).isEqualTo("hong.g@iam.com");
                assertThat(user.getFamilyName()).isEqualTo("Hong");

                var extData = user.getExtension().getExtensions()
                                .get("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");
                assertThat(extData).isInstanceOf(EnterpriseUserExtension.class);
                assertThat(((EnterpriseUserExtension) extData).getDepartment()).isEqualTo("Dev Team");
                assertThat(((EnterpriseUserExtension) extData).getEmployeeNumber()).isEqualTo("H001");
        }

        @Test
        @DisplayName("기존 사용자 정보 변경 시 Core 컬럼과 Extension JSONB가 모두 업데이트 되어야 한다")
        void processHrSync_UpdateUser_ShouldUpdateHybrid() {
                // Given
                String hrEmpId = "H002";
                Map<String, Object> firstPayload = Map.of(
                                "externalId", hrEmpId,
                                "email", "kim.f@iam.com",
                                "lastName", "Kim",
                                "firstName", "Free",
                                "title", "Junior",
                                "active", true,
                                "empNo", hrEmpId,
                                "deptName", "Dev Team");

                userSyncService.processSync(
                                new UserSyncEvent("trace-1", SYSTEM_ID, SyncConstants.EVENT_USER_CREATE,
                                                LocalDateTime.now(), firstPayload));

                Map<String, Object> updatePayload = Map.of(
                                "externalId", hrEmpId,
                                "email", "kim.f@iam.com",
                                "lastName", "Kim",
                                "firstName", "Future",
                                "title", "Senior",
                                "active", true,
                                "empNo", hrEmpId,
                                "deptName", "IT Team");

                // When
                userSyncService.processSync(
                                new UserSyncEvent("trace-2", SYSTEM_ID, SyncConstants.EVENT_USER_UPDATE,
                                                LocalDateTime.now(),
                                                updatePayload));

                // Then
                var link = identityLinkRepository.findBySystemTypeAndExternalId(SYSTEM_ID, hrEmpId).get();
                Long userId = link.getIamUserId();
                assertThat(userId).isNotNull();
                var user = iamUserRepository.findById(userId).get();

                assertThat(user.getGivenName()).isEqualTo("Future");
                assertThat(user.getTitle()).isEqualTo("Senior");

                var extData = user.getExtension().getExtensions()
                                .get("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");
                assertThat(extData).isInstanceOf(EnterpriseUserExtension.class);
                assertThat(((EnterpriseUserExtension) extData).getDepartment()).isEqualTo("IT Team");
        }

        @Test
        @DisplayName("동기화 실패 시 보상 트랜잭션 이벤트가 발행되어야 한다")
        void processSync_OnError_ShouldPublishCompensationEvent() {
                // Given
                String hrEmpId = "ERR001";
                // email (mandatory) is missing to trigger validation error
                Map<String, Object> invalidPayload = Map.of(
                                "externalId", hrEmpId,
                                "lastName", "Error",
                                "firstName", "Case",
                                "title", "Tester",
                                "active", true,
                                "empNo", hrEmpId,
                                "deptName", "QA");

                UserSyncEvent event = new UserSyncEvent("trace-error-1", SYSTEM_ID, SyncConstants.EVENT_USER_CREATE,
                                LocalDateTime.now(), invalidPayload);

                // When & Then
                org.junit.jupiter.api.Assertions.assertThrows(
                                com.iam.core.domain.exception.TransformationException.class,
                                () -> {
                                        userSyncService.processSync(event);
                                });

                // Verify Compensation Event Published
                org.mockito.Mockito.verify(messagePublisher, org.mockito.Mockito.times(1))
                                .publish(
                                                org.mockito.ArgumentMatchers.eq("iam.topic"),
                                                org.mockito.ArgumentMatchers.eq("iam.event.compensation"),
                                                org.mockito.ArgumentMatchers.any(
                                                                com.iam.core.domain.event.SyncCompensationEvent.class));
        }
}