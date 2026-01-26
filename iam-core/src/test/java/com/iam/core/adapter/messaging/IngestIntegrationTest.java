package com.iam.core.adapter.messaging;

import com.iam.core.application.common.UserSyncEvent;
import com.iam.core.application.sync.TransMappingService;
import com.iam.core.domain.common.port.MessagePublisher;
import com.iam.core.domain.sync.TransFieldMapping;
import com.iam.core.domain.sync.TransMapping;
import com.iam.core.domain.sync.TransRuleMeta;
import com.iam.core.domain.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
class IngestIntegrationTest {

        @MockitoBean
        private MessagePublisher messagePublisher;

        @Autowired
        private IngestListener ingestListener;

        @Autowired
        private IamUserRepository iamUserRepository;

        @Autowired
        private IdentityLinkRepository identityLinkRepository;

        @Autowired
        private TransMappingService transMappingService;

        private static final String SYSTEM_ID = "TEST_HR_INGEST";
        private static final String RULE_ID = "TEST_INGEST_RULE";

        @BeforeEach
        void setUp() {
                if (transMappingService.getMappings(RULE_ID).isEmpty()) {
                        // 1. Rule Meta
                        transMappingService.saveRuleMeta(TransRuleMeta.builder()
                                        .ruleId(RULE_ID)
                                        .ruleName("Test Ingest Rule")
                                        .targetAttribute("CORE")
                                        .status("ACTIVE")
                                        .build());

                        // 2. Trans Mapping
                        transMappingService.saveTransMapping(TransMapping.builder()
                                        .systemId(SYSTEM_ID)
                                        .ruleId(RULE_ID)
                                        .execOrder(1)
                                        .isMandatory(true)
                                        .build());

                        // 3. Field Mappings (Triggers script generation)
                        java.util.List<TransFieldMapping> mappings = java.util.List.of(
                                        TransFieldMapping.builder().ruleId(RULE_ID)
                                                        .sourceField("empNo").targetField("userName").isRequired(true)
                                                        .build(),
                                        TransFieldMapping.builder().ruleId(RULE_ID)
                                                        .sourceField("lastName").targetField("familyName").build(),
                                        TransFieldMapping.builder().ruleId(RULE_ID)
                                                        .sourceField("firstName").targetField("givenName").build(),
                                        TransFieldMapping.builder().ruleId(RULE_ID)
                                                        .sourceField("position").targetField("title").build(),
                                        TransFieldMapping.builder().ruleId(RULE_ID)
                                                        .sourceField("active").targetField("active").build(),
                                        TransFieldMapping.builder().ruleId(RULE_ID)
                                                        .sourceField("empNo").targetField("employeeNumber").build());

                        mappings.forEach(transMappingService::saveMapping);
                }
        }

        @Test
        @DisplayName("Raw HR Data Ingestion -> Transformation -> IAM User Creation")
        void testIngestionFlow() throws Exception {
                // Given
                Map<String, Object> payload = Map.of(
                                "externalId", "EMP001",
                                "empNo", "20230001",
                                "firstName", "Gildong",
                                "lastName", "Hong",
                                "position", "Engineer",
                                "active", true);
                UserSyncEvent userSyncEvent = new UserSyncEvent("test-trace-1", SYSTEM_ID, "USER_CREATE", LocalDateTime.now(), payload);

                // When
                ingestListener.onRawDataIngested(userSyncEvent);

                // Then
                // 1. Check IdentityLink
                Optional<IdentityLink> linkOpt = identityLinkRepository.findBySystemTypeAndExternalId(SYSTEM_ID,
                                "EMP001");
                assertThat(linkOpt).isPresent();
                Long userId = linkOpt.get().getIamUserId();

                // 2. Check IamUser
                Optional<IamUser> userOpt = iamUserRepository.findById(userId);
                assertThat(userOpt).isPresent();
                IamUser user = userOpt.get();

                assertThat(user.getUserName()).isEqualTo("20230001");
                assertThat(user.getFamilyName()).isEqualTo("Hong");
                assertThat(user.getGivenName()).isEqualTo("Gildong");
                assertThat(user.getTitle()).isEqualTo("Engineer");
                assertThat(user.isActive()).isTrue();
                assertThat(user.getExternalId()).isEqualTo("EMP001");

                // 3. Check Extensions
                assertThat(user.getExtension()).isNotNull();
                EnterpriseUserExtension enterprise = (EnterpriseUserExtension) user.getExtension().getExtensions()
                                .get("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");
                assertThat(enterprise).isNotNull();
                assertThat(enterprise.getEmployeeNumber()).isEqualTo("20230001");
        }
}
