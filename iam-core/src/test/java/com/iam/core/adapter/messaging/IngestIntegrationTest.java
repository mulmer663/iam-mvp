package com.iam.core.adapter.messaging;

import com.iam.core.domain.entity.*;
import com.iam.core.domain.port.MessagePublisher;
import com.iam.core.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

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
        private TransRuleMetaRepository transRuleMetaRepository;

        @Autowired
        private TransRuleVersionRepository transRuleVersionRepository;

        @Autowired
        private TransMappingRepository transMappingRepository;

        @BeforeEach
        void setUp() {
                // Setup a rule for SAP_HR
                String ruleId = "TEST_CORE_TRANS";

                TransRuleMeta meta = TransRuleMeta.builder()
                                .ruleId(ruleId)
                                .ruleName("TEST HR Core Transformation")
                                .targetAttribute("CORE")
                                .status("ACTIVE")
                                .build();
                transRuleMetaRepository.save(meta);

                String script = """
                                    def res = [:]
                                    res.userName = source.empNo.asString()
                                    res.familyName = source.lastName.asString()
                                    res.givenName = source.firstName.asString()
                                    res.title = source.position.asString()
                                    res.active = new com.iam.core.domain.vo.BooleanData(true)
                                    res.employeeNumber = source.empNo.asString()
                                    res.externalId = source.externalId.asString()
                                    return res
                                """;

                TransRuleVersion version = TransRuleVersion.builder()
                                .ruleId(ruleId)
                                .versionNo(1)
                                .scriptContent(script)
                                .scriptHash("hash123")
                                .isCurrent(true)
                                .build();
                transRuleVersionRepository.save(version);

                TransMapping mapping = TransMapping.builder()
                                .systemId("TEST_HR")
                                .ruleId(ruleId)
                                .execOrder(1)
                                .isMandatory(true)
                                .build();
                transMappingRepository.save(mapping);
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
                                "position", "Engineer");
                Map<String, Object> event = Map.of(
                                "traceId", "test-trace-1",
                                "systemId", "TEST_HR",
                                "payload", payload);
                // When
                ingestListener.onRawDataIngested(event);

                // Then
                // 1. Check IdentityLink
                Optional<IdentityLink> linkOpt = identityLinkRepository.findBySystemTypeAndExternalId("TEST_HR",
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
