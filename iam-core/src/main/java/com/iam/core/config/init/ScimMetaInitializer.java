package com.iam.core.config.init;

import com.iam.core.domain.entity.IamAttributeMeta;
import com.iam.core.domain.entity.ScimResourceTypeMeta;
import com.iam.core.domain.entity.ScimSchemaMeta;
import com.iam.core.domain.enums.AttributeCategory;
import com.iam.core.domain.enums.AttributeDataType;
import com.iam.core.domain.enums.AttributeMutability;
import com.iam.core.domain.enums.AttributeTargetDomain;
import com.iam.core.domain.repository.IamAttributeMetaRepository;
import com.iam.core.domain.repository.ScimResourceTypeMetaRepository;
import com.iam.core.domain.repository.ScimSchemaMetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SCIM 메타데이터 및 속성 정의 이니셜라이저.
 * 가장 먼저 실행되어 스키마를 정의합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
@Profile({ "local", "dev", "test", "default" })
public class ScimMetaInitializer implements CommandLineRunner {

    private final IamAttributeMetaRepository attributeMetaRepository;
    private final ScimSchemaMetaRepository scimSchemaMetaRepository;
    private final ScimResourceTypeMetaRepository scimResourceTypeMetaRepository;

    @Override
    public void run(String... args) {
        initScimMetadata();
        initAttributeMeta();
    }

    private void initScimMetadata() {
        if (scimSchemaMetaRepository.count() > 0) {
            return;
        }

        log.info("🚀 [ScimMetaInitializer] SCIM 메타 데이터 생성을 시작합니다...");

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

        log.info("✅ [ScimMetaInitializer] SCIM 메타 데이터를 생성했습니다.");
    }

    private void initAttributeMeta() {
        if (attributeMetaRepository.count() > 0) {
            return;
        }

        log.info("🚀 [ScimMetaInitializer] 속성 메타 데이터 검사 및 생성을 시작합니다...");

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
        log.info("✅ [ScimMetaInitializer] 기본 속성 메타 데이터를 생성했습니다.");
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
}
