package com.iam.registry.config.init;

import com.iam.registry.domain.common.constant.ScimEndpointConstants;
import com.iam.registry.domain.common.enums.AttributeCategory;
import com.iam.registry.domain.common.enums.AttributeDataType;
import com.iam.registry.domain.common.enums.AttributeMutability;
import com.iam.registry.domain.common.enums.AttributeTargetDomain;
import com.iam.registry.domain.scim.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Seeds SCIM 2.0 standard schemas, resource types, and core/extension
 * attribute metadata at startup. With ddl-auto=create-drop this runs
 * against a clean DB on every boot; the count-guards inside are retained
 * so the class is also safe under ddl-auto=update.
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

                log.info("[ScimMetaInitializer] Seeding SCIM metadata...");

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

                scimResourceTypeMetaRepository.saveAll(List.of(
                                ScimResourceTypeMeta.builder()
                                                .id(ScimEndpointConstants.USERS)
                                                .name("User")
                                                .description("User Account")
                                                .endpoint("/Users")
                                                .schema(userSchemaUri)
                                                .schemaExtensions(new LinkedHashSet<>(List.of(
                                                                ScimResourceTypeExtension.builder()
                                                                                .schema(enterpriseSchemaUri)
                                                                                .required(false)
                                                                                .build())))
                                                .build(),
                                ScimResourceTypeMeta.builder()
                                                .id(ScimEndpointConstants.GROUPS)
                                                .name("Group")
                                                .description("Group")
                                                .endpoint("/Groups")
                                                .schema(groupSchemaUri)
                                                .build()));

                log.info("[ScimMetaInitializer] SCIM metadata seeded.");
        }

        private void initAttributeMeta() {
                if (attributeMetaRepository.count() > 0) {
                        return;
                }

                log.info("[ScimMetaInitializer] Seeding attribute metadata...");

                List<IamAttributeMeta> attributes = new ArrayList<>();

                // CORE Attributes - User singular
                attributes.add(createAttr("userName", AttributeTargetDomain.USER, AttributeCategory.CORE, "User ID",
                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                "Unique Identifier", true, AttributeMutability.IMMUTABLE, "text-input", false));
                attributes.add(createAttr("familyName", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                "Last Name", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:core:2.0:User", "Family Name", true,
                                AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("givenName", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                "First Name", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:core:2.0:User", "Given Name", true,
                                AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("displayName", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                "Display Name", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:core:2.0:User", "User's display name", false,
                                AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("nickName", AttributeTargetDomain.USER, AttributeCategory.CORE, "Nick Name",
                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                "User's nickname", false, AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("profileUrl", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                "Profile URL", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:core:2.0:User",
                                "URL of the user's online profile", false,
                                AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("title", AttributeTargetDomain.USER, AttributeCategory.CORE, "Job Title",
                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                "Job Title / Position", false, AttributeMutability.READ_WRITE, "text-input",
                                false));
                attributes.add(createAttr("userType", AttributeTargetDomain.USER, AttributeCategory.CORE, "User Type",
                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                "Relationship to organization", false, AttributeMutability.READ_WRITE,
                                "text-input", false));
                attributes.add(createAttr("preferredLanguage", AttributeTargetDomain.USER, AttributeCategory.CORE,
                                "Preferred Language", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:core:2.0:User", "Preferred language", false,
                                AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("locale", AttributeTargetDomain.USER, AttributeCategory.CORE, "Locale",
                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                "User's locale", false, AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("timezone", AttributeTargetDomain.USER, AttributeCategory.CORE, "Timezone",
                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                "User's timezone", false, AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("active", AttributeTargetDomain.USER, AttributeCategory.CORE, "Active",
                                AttributeDataType.BOOLEAN, "urn:ietf:params:scim:schemas:core:2.0:User",
                                "Account Status", true, AttributeMutability.READ_WRITE, "toggle", false));
                attributes.add(createAttr("password", AttributeTargetDomain.USER, AttributeCategory.CORE, "Password",
                                AttributeDataType.STRING, "urn:ietf:params:scim:schemas:core:2.0:User",
                                "User's password", false, AttributeMutability.READ_WRITE, "password-input",
                                false));

                // CORE Attributes - User Multi-Valued (COMPLEX with sub-attributes)
                addComplexAttribute(attributes, "emails", "Emails", "Email addresses", true,
                                "value", "display", "type", "primary");
                addComplexAttribute(attributes, "phoneNumbers", "Phone Numbers", "Phone numbers", true,
                                "value", "display", "type", "primary");
                addComplexAttribute(attributes, "ims", "IMs", "Instant messaging addresses", true,
                                "value", "display", "type", "primary");
                addComplexAttribute(attributes, "photos", "Photos", "User photos", true,
                                "value", "display", "type", "primary");
                addComplexAttribute(attributes, "entitlements", "Entitlements", "User entitlements", true,
                                "value", "display", "type", "primary");
                addComplexAttribute(attributes, "roles", "Roles", "User roles", true,
                                "value", "display", "type", "primary");
                addComplexAttribute(attributes, "x509Certificates", "X.509 Certificates", "User certificates", true,
                                "value", "display", "type", "primary");

                // Addresses
                attributes.add(createAttr("addresses", AttributeTargetDomain.USER, AttributeCategory.CORE, "Addresses",
                                AttributeDataType.COMPLEX, "urn:ietf:params:scim:schemas:core:2.0:User",
                                "Physical addresses", false, AttributeMutability.READ_WRITE, "multi-input", true));
                attributes.addAll(createSubAttrs("addresses", "urn:ietf:params:scim:schemas:core:2.0:User",
                                AttributeTargetDomain.USER,
                                "formatted", "streetAddress", "locality", "region", "postalCode", "country", "type",
                                "primary"));

                // Groups (read-only)
                attributes.add(createAttr("groups", AttributeTargetDomain.USER, AttributeCategory.CORE, "Groups",
                                AttributeDataType.COMPLEX, "urn:ietf:params:scim:schemas:core:2.0:User",
                                "User groups (Read-only)", false, AttributeMutability.READ_ONLY, "list-view", true));
                attributes.addAll(createSubAttrs("groups", "urn:ietf:params:scim:schemas:core:2.0:User",
                                AttributeTargetDomain.USER,
                                "value", "$ref", "display", "type"));

                // EXTENSION Attributes - Enterprise User
                attributes.add(createAttr("employeeNumber", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                "Employee Number", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                "Numeric or alphanumeric identifier for the user", true,
                                AttributeMutability.IMMUTABLE, "text-input", false));
                attributes.add(createAttr("costCenter", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                "Cost Center", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                "Identifies the name of a cost center", false,
                                AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("organization", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                "Organization", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                "Identifies the name of an organization", false,
                                AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("division", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                "Division", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                "Identifies the name of a division", false,
                                AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("department", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                "Department", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                "Identifies the name of a department", false,
                                AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("managerId", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                "Manager ID", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                "The id of the user's manager", false,
                                AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("managerRef", AttributeTargetDomain.USER, AttributeCategory.EXTENSION,
                                "Manager URI", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                "The URI of the user's manager", false,
                                AttributeMutability.READ_WRITE, "text-input", false));
                attributes.add(createAttr("managerDisplayName", AttributeTargetDomain.USER,
                                AttributeCategory.EXTENSION, "Manager Name", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                "The displayName of the user's manager", false,
                                AttributeMutability.READ_ONLY, "text-input", false));

                // CORE Attributes - Group
                attributes.add(createAttr("displayName", AttributeTargetDomain.GROUP, AttributeCategory.CORE,
                                "Display Name", AttributeDataType.STRING,
                                "urn:ietf:params:scim:schemas:core:2.0:Group",
                                "A human-readable name for the Group", true,
                                AttributeMutability.READ_WRITE, "text-input", false));

                // Group members
                attributes.add(createAttr("members", AttributeTargetDomain.GROUP, AttributeCategory.CORE, "Members",
                                AttributeDataType.COMPLEX, "urn:ietf:params:scim:schemas:core:2.0:Group",
                                "Group members", false, AttributeMutability.READ_WRITE, "multi-input", true));
                attributes.addAll(createSubAttrs("members", "urn:ietf:params:scim:schemas:core:2.0:Group",
                                AttributeTargetDomain.GROUP,
                                "value", "$ref", "display", "type"));

                // RFC 7643 §4.1/4.2: enrich type sub-attributes with canonical values
                // and reference sub-attributes with referenceTypes.
                enrichSubAttr(attributes, "emails", "type", AttributeTargetDomain.USER,
                                List.of("work", "home", "other"), null);
                enrichSubAttr(attributes, "phoneNumbers", "type", AttributeTargetDomain.USER,
                                List.of("work", "home", "mobile", "fax", "pager", "other"), null);
                enrichSubAttr(attributes, "ims", "type", AttributeTargetDomain.USER,
                                List.of("aim", "gtalk", "icq", "xmpp", "msn", "skype", "qq", "yahoo"), null);
                enrichSubAttr(attributes, "photos", "type", AttributeTargetDomain.USER,
                                List.of("photo", "thumbnail"), null);
                enrichSubAttr(attributes, "photos", "value", AttributeTargetDomain.USER,
                                null, List.of("external"));
                enrichSubAttr(attributes, "addresses", "type", AttributeTargetDomain.USER,
                                List.of("work", "home", "other"), null);
                // addresses.formatted is a long, single-line/multi-line readable address.
                // Hint the UI to render it as a full-width textarea cell.
                setSubAttrUiComponent(attributes, "addresses", "formatted",
                                AttributeTargetDomain.USER, "textarea");
                enrichSubAttr(attributes, "groups", "type", AttributeTargetDomain.USER,
                                List.of("direct", "indirect"), null);
                enrichSubAttr(attributes, "groups", "$ref", AttributeTargetDomain.USER,
                                null, List.of("Group"));
                enrichSubAttr(attributes, "members", "type", AttributeTargetDomain.GROUP,
                                List.of("User", "Group"), null);
                enrichSubAttr(attributes, "members", "$ref", AttributeTargetDomain.GROUP,
                                null, List.of("User", "Group"));

                attributeMetaRepository.saveAll(attributes);
                log.info("[ScimMetaInitializer] Seeded {} attribute metas.", attributes.size());
        }

        /**
         * Apply RFC 7643 canonicalValues / referenceTypes onto a previously-seeded
         * sub-attribute identified by (parent.subName, domain). Pass null for
         * either list to leave that field untouched.
         */
        private void enrichSubAttr(List<IamAttributeMeta> seed, String parent, String subName,
                        AttributeTargetDomain domain,
                        List<String> canonicalValues, List<String> referenceTypes) {
                String fullName = parent + "." + subName;
                for (IamAttributeMeta a : seed) {
                        if (fullName.equals(a.getName()) && a.getTargetDomain() == domain) {
                                if (canonicalValues != null) {
                                        a.setCanonicalValues(new ArrayList<>(canonicalValues));
                                }
                                if (referenceTypes != null) {
                                        a.setReferenceTypes(new ArrayList<>(referenceTypes));
                                }
                                return;
                        }
                }
                log.warn("[ScimMetaInitializer] enrichSubAttr: {} ({}) not found in seed", fullName, domain);
        }

        /**
         * Set uiComponent metadata on a previously-seeded sub-attribute. The value
         * is a free-form hint consumed by the frontend (e.g. "textarea" promotes
         * the cell to a full-row text block in the dynamic User form).
         */
        private void setSubAttrUiComponent(List<IamAttributeMeta> seed, String parent, String subName,
                        AttributeTargetDomain domain, String uiComponent) {
                String fullName = parent + "." + subName;
                for (IamAttributeMeta a : seed) {
                        if (fullName.equals(a.getName()) && a.getTargetDomain() == domain) {
                                a.setUiComponent(uiComponent);
                                return;
                        }
                }
                log.warn("[ScimMetaInitializer] setSubAttrUiComponent: {} ({}) not found in seed", fullName, domain);
        }

        private void addComplexAttribute(List<IamAttributeMeta> attributes, String parentName, String displayName,
                        String desc, boolean required, String... subAttributeNames) {
                String schema = "urn:ietf:params:scim:schemas:core:2.0:User";
                attributes.add(createAttr(parentName, AttributeTargetDomain.USER, AttributeCategory.CORE, displayName,
                                AttributeDataType.COMPLEX, schema, desc, required, AttributeMutability.READ_WRITE,
                                "multi-input", true));
                attributes.addAll(createSubAttrs(parentName, schema, AttributeTargetDomain.USER, subAttributeNames));
        }

        private List<IamAttributeMeta> createSubAttrs(String parentName, String schema,
                        AttributeTargetDomain domain, String... subNames) {
                // Persisted id is parent.child to keep each sub-attribute row unique
                // ((name, targetDomain) is the composite @Id on IamAttributeMeta).
                // ScimSchemaService strips the prefix when emitting the SCIM-shaped name.
                List<IamAttributeMeta> subs = new ArrayList<>();
                for (String subName : subNames) {
                        String id = parentName + "." + subName;
                        subs.add(IamAttributeMeta.builder()
                                        .name(id)
                                        .parentName(parentName)
                                        .targetDomain(domain)
                                        .category(AttributeCategory.CORE)
                                        .displayName(subName)
                                        .type(AttributeDataType.STRING)
                                        .scimSchemaUri(schema)
                                        .description("Sub-attribute " + subName + " of " + parentName)
                                        .required(false)
                                        .mutability(AttributeMutability.READ_WRITE)
                                        .uiComponent("text-input")
                                        .multiValued(false)
                                        .build());
                }
                return subs;
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
