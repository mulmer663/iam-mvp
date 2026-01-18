package com.iam.core.config;

import com.iam.core.application.dto.UserSyncEvent;
import com.iam.core.application.service.IamUserUpdateService;
import com.iam.core.application.service.SyncHistoryService;
import com.iam.core.application.service.TransMappingService;
import com.iam.core.domain.constant.AttributeConstants;
import com.iam.core.domain.constant.SystemConstants;
import com.iam.core.domain.entity.*;
import com.iam.core.domain.enums.AttributeCategory;
import com.iam.core.domain.enums.AttributeDataType;
import com.iam.core.domain.enums.AttributeMutability;
import com.iam.core.domain.enums.AttributeTargetDomain;
import com.iam.core.domain.repository.IamAttributeMetaRepository;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.ScimResourceTypeMetaRepository;
import com.iam.core.domain.repository.ScimSchemaMetaRepository;
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
        private final IamAttributeMetaRepository attributeMetaRepository;
        private final ScimSchemaMetaRepository scimSchemaMetaRepository;
        private final ScimResourceTypeMetaRepository scimResourceTypeMetaRepository;

        @Override
        public void run(String... args) {
                initRuleEngineData();
                initScimMetadata();
                initAttributeMeta();

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
                        createUserViaService("super.admin", "Michael", "Admin", "IT Director", true,
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

        private void initAttributeMeta() {
                if (attributeMetaRepository.count() > 0) {
                        return;
                }

                log.info("🚀 [DataInitializer] 속성 메타 데이터 검사 및 생성을 시작합니다...");

                List<IamAttributeMeta> attributes = List.of(
                                // CORE Attributes - User Singular
                                createAttr("userName", AttributeTargetDomain.USER, AttributeCategory.CORE, "User ID",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "Unique Identifier", true, AttributeMutability.IMMUTABLE, "text-input",
                                                false),
                                createAttr("familyName", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                                "Last Name",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "Family Name", true, AttributeMutability.READ_WRITE, "text-input",
                                                false),
                                createAttr("givenName", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                                "First Name",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "Given Name", true, AttributeMutability.READ_WRITE, "text-input",
                                                false),
                                createAttr("displayName", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                                "Display Name", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:core:2.0:User", "User's display name",
                                                false, AttributeMutability.READ_WRITE, "text-input", false),
                                createAttr("nickName", AttributeTargetDomain.USER, AttributeCategory.CORE, "Nick Name",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "User's nickname", false, AttributeMutability.READ_WRITE, "text-input",
                                                false),
                                createAttr("profileUrl", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                                "Profile URL", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "URL of the user's online profile", false,
                                                AttributeMutability.READ_WRITE, "text-input", false),
                                createAttr("title", AttributeTargetDomain.USER, AttributeCategory.CORE, "Job Title",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "Job Title / Position", false, AttributeMutability.READ_WRITE,
                                                "text-input", false),
                                createAttr("userType", AttributeTargetDomain.USER, AttributeCategory.CORE, "User Type",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "Relationship to organization", false, AttributeMutability.READ_WRITE,
                                                "text-input", false),
                                createAttr("preferredLanguage", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                                "Preferred Language", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:core:2.0:User", "Preferred language",
                                                false, AttributeMutability.READ_WRITE, "text-input", false),
                                createAttr("locale", AttributeTargetDomain.USER, AttributeCategory.CORE, "Locale",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "User's locale", false, AttributeMutability.READ_WRITE, "text-input",
                                                false),
                                createAttr("timezone", AttributeTargetDomain.USER, AttributeCategory.CORE, "Timezone",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "User's timezone", false, AttributeMutability.READ_WRITE, "text-input",
                                                false),
                                createAttr("active", AttributeTargetDomain.USER, AttributeCategory.CORE, "Active",
                                                AttributeDataType.BOOLEAN, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "Account Status", true, AttributeMutability.READ_WRITE, "toggle",
                                                false),
                                createAttr("password", AttributeTargetDomain.USER, AttributeCategory.CORE, "Password",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "User's password", false, AttributeMutability.READ_WRITE,
                                                "password-input", false),

                                // CORE Attributes - User Multi-Valued
                                createAttr("emails", AttributeTargetDomain.USER, AttributeCategory.CORE, "Emails",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "Email addresses", false, AttributeMutability.READ_WRITE,
                                                "multi-input", true),
                                createAttr("phoneNumbers", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                                "Phone Numbers", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:core:2.0:User", "Phone numbers", false,
                                                AttributeMutability.READ_WRITE, "multi-input", true),
                                createAttr("ims", AttributeTargetDomain.USER, AttributeCategory.CORE, "IMs",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "IM addresses", false, AttributeMutability.READ_WRITE, "multi-input",
                                                true),
                                createAttr("photos", AttributeTargetDomain.USER, AttributeCategory.CORE, "Photos",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "User photos", false, AttributeMutability.READ_WRITE, "multi-input",
                                                true),
                                createAttr("addresses", AttributeTargetDomain.USER, AttributeCategory.CORE, "Addresses",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "Physical addresses", false, AttributeMutability.READ_WRITE,
                                                "multi-input", true),
                                createAttr("groups", AttributeTargetDomain.USER, AttributeCategory.CORE, "Groups",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "User groups (Read-only)", false, AttributeMutability.READ_ONLY,
                                                "list-view", true),
                                createAttr("entitlements", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                                "Entitlements", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:core:2.0:User", "User entitlements",
                                                false, AttributeMutability.READ_WRITE, "multi-input", true),
                                createAttr("roles", AttributeTargetDomain.USER, AttributeCategory.CORE, "Roles",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                                "User roles", false, AttributeMutability.READ_WRITE, "multi-input",
                                                true),
                                createAttr("x509Certificates", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                                "X.509 Certificates", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:core:2.0:User", "User certificates",
                                                false, AttributeMutability.READ_WRITE, "multi-input", true),

                                // EXTENSION Attributes - User Enterprise
                                createAttr("employeeNumber", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                                "Employee Number", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                                "Numeric or alphanumeric identifier for the user", true,
                                                AttributeMutability.IMMUTABLE, "text-input", false),
                                createAttr("costCenter", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                                "Cost Center", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                                "Identifies the name of a cost center", false,
                                                AttributeMutability.READ_WRITE, "text-input", false),
                                createAttr("organization", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                                "Organization", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                                "Identifies the name of an organization", false,
                                                AttributeMutability.READ_WRITE, "text-input", false),
                                createAttr("division", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                                "Division", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                                "Identifies the name of a division", false,
                                                AttributeMutability.READ_WRITE, "text-input", false),
                                createAttr("department", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                                "Department", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                                "Identifies the name of a department", false,
                                                AttributeMutability.READ_WRITE, "text-input", false),
                                createAttr("managerId", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                                "Manager ID", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                                "The id of the user's manager", false, AttributeMutability.READ_WRITE,
                                                "text-input", false),
                                createAttr("managerRef", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                                "Manager URI", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                                "The URI of the user's manager", false, AttributeMutability.READ_WRITE,
                                                "text-input", false),
                                createAttr("managerDisplayName", AttributeTargetDomain.USER,
                                                AttributeCategory.EXTENSION, "Manager Name", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                                "The displayName of the user's manager", false,
                                                AttributeMutability.READ_ONLY, "text-input", false),

                                // CORE Attributes - Group
                                createAttr("displayName", AttributeTargetDomain.GROUP, AttributeCategory.CORE,
                                                "Display Name", AttributeDataType.STRING,
                                                "urn:ietf:params:scim:schemas:core:2.0:Group",
                                                "A human-readable name for the Group", true,
                                                AttributeMutability.READ_WRITE, "text-input", false),
                                createAttr("members", AttributeTargetDomain.GROUP, AttributeCategory.CORE, "Members",
                                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:Group",
                                                "Group members", false, AttributeMutability.READ_WRITE, "multi-input",
                                                true));

                attributeMetaRepository.saveAll(attributes);
                log.info("✅ [DataInitializer] 기본 속성 메타 데이터를 생성했습니다.");
        }

        private IamAttributeMeta createAttr(String name, AttributeTargetDomain domain, AttributeCategory category,
                        String displayName, AttributeDataType type, String schema, String desc, boolean required,
                        AttributeMutability mutability, String ui, boolean multiValued) {
                return IamAttributeMeta.builder()
                                .name(name)
                                .targetDomain(domain)
                                .category(category)
                                .displayName(displayName)
                                .type(type)
                                .scimSchemaUri(schema)
                                .description(desc)
                                .required(required)
                                .mutability(mutability)
                                .uiComponent(ui)
                                .multiValued(multiValued)
                                .build();
        }

        private void initScimMetadata() {
                if (scimSchemaMetaRepository.count() > 0) {
                        return;
                }

                log.info("🚀 [DataInitializer] SCIM 메타 데이터 생성을 시작합니다...");

                // 1. Schemas
                String userSchemaUri = "urn:ietf:params:scim:schemas:core:2.0:User";
                String enterpriseSchemaUri = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";
                String groupSchemaUri = "urn:ietf:params:scim:schemas:core:2.0:Group";

                scimSchemaMetaRepository.saveAll(List.of(
                                ScimSchemaMeta.builder()
                                                .id(userSchemaUri)
                                                .name("User")
                                                .description("Core User Schema")
                                                .build(),
                                ScimSchemaMeta.builder()
                                                .id(enterpriseSchemaUri)
                                                .name("EnterpriseUser")
                                                .description("Enterprise User Extension")
                                                .build(),
                                ScimSchemaMeta.builder()
                                                .id(groupSchemaUri)
                                                .name("Group")
                                                .description("Core Group Schema")
                                                .build()));

                // 2. ResourceTypes
                scimResourceTypeMetaRepository.saveAll(List.of(
                                ScimResourceTypeMeta.builder()
                                                .id("User")
                                                .name("User")
                                                .description("User Account")
                                                .endpoint("/Users")
                                                .schema(userSchemaUri)
                                                .schemaExtensions(List.of(enterpriseSchemaUri))
                                                .build(),
                                ScimResourceTypeMeta.builder()
                                                .id("Group")
                                                .name("Group")
                                                .description("Group")
                                                .endpoint("/Groups")
                                                .schema(groupSchemaUri)
                                                .build()));

                log.info("✅ [DataInitializer] SCIM 메타 데이터를 생성했습니다.");
        }
}