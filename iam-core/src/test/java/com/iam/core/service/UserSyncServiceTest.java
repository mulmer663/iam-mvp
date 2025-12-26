package com.iam.core.service;

import com.iam.core.domain.entity.EnterpriseUserExtension;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.IdentityLinkRepository;
import com.iam.core.dto.UserSyncEvent;
import com.iam.core.dto.UserSyncPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserSyncServiceTest {

        @Autowired
        private UserSyncService userSyncService;

        @MockitoBean
        private RabbitTemplate rabbitTemplate;

        @Autowired
        private IamUserRepository iamUserRepository;

        @Autowired
        private IdentityLinkRepository identityLinkRepository;

        @Test
        @DisplayName("HR 신규 사용자 동기화 시 DB에 정상 저장되고 컬럼과 JSONB가 분리되어야 한다")
        void processHrSync_NewUser_ShouldSaveHybrid() {
                // Given
                String hrEmpId = "H001";
                var payload = new UserSyncPayload(
                                hrEmpId,
                                "hong.g@iam.com",
                                new UserSyncPayload.Name("Hong", "Gildong", "Hong Gildong"),
                                "Senior Engineer",
                                true,
                                Map.of("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                                Map.of("employeeNumber", "H001", "department", "Dev Team")));

                UserSyncEvent event = new UserSyncEvent("trace-123", "SYNC_USER", LocalDateTime.now(), payload);

                // When
                userSyncService.processHrSync(event);

                // Then
                var link = identityLinkRepository.findBySystemTypeAndExternalId("HR", hrEmpId).get();
                var user = iamUserRepository.findById(link.getIamUserId()).get();

                assertThat(user.getUserName()).isEqualTo("hong.g@iam.com");
                assertThat(user.getFamilyName()).isEqualTo("Hong");

                var extData = user.getExtension().getExtensions()
                                .get("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");
                assertThat(extData).isInstanceOf(EnterpriseUserExtension.class);
                assertThat(((EnterpriseUserExtension) extData).getDepartment()).isEqualTo("Dev Team");
        }

        @Test
        @DisplayName("기존 사용자 정보 변경 시 Core 컬럼과 Extension JSONB가 모두 업데이트 되어야 한다")
        void processHrSync_UpdateUser_ShouldUpdateHybrid() {
                // Given
                String hrEmpId = "H002";
                var firstPayload = new UserSyncPayload(
                                hrEmpId, "kim.f@iam.com",
                                new UserSyncPayload.Name("Kim", "Free", "Kim Free"),
                                "Junior", true, Map.of());
                userSyncService.processHrSync(new UserSyncEvent("trace-1", "SYNC", LocalDateTime.now(), firstPayload));

                var updatePayload = new UserSyncPayload(
                                hrEmpId, "kim.f@iam.com",
                                new UserSyncPayload.Name("Kim", "Future", "Kim Future"),
                                "Senior", true,
                                Map.of("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                                Map.of("department", "IT Team")));

                // When
                userSyncService.processHrSync(
                                new UserSyncEvent("trace-2", "UPDATE", LocalDateTime.now(), updatePayload));

                // Then
                var link = identityLinkRepository.findBySystemTypeAndExternalId("HR", hrEmpId).get();
                var user = iamUserRepository.findById(link.getIamUserId()).get();

                assertThat(user.getGivenName()).isEqualTo("Future");
                assertThat(user.getTitle()).isEqualTo("Senior");

                var extData = user.getExtension().getExtensions()
                                .get("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");
                assertThat(extData).isInstanceOf(EnterpriseUserExtension.class);
                assertThat(((EnterpriseUserExtension) extData).getDepartment()).isEqualTo("IT Team");
        }
}
