package com.iam.core.application.service;

import com.iam.core.domain.entity.EnterpriseUserExtension;
import com.iam.core.domain.port.MessagePublisher;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.IdentityLinkRepository;
import com.iam.core.application.dto.UserSyncEvent;
import com.iam.core.application.dto.UserSyncPayload;
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
        private IdentityLinkRepository identityLinkRepository;

        @Test
        @DisplayName("HR 신규 사용자 동기화 시 DB에 정상 저장되고 컬럼과 JSONB가 분리되어야 한다")
        void processHrSync_NewUser_ShouldSaveHybrid() {
                // Given
                String hrEmpId = "H001";
                var payload = UserSyncPayload.builder()
                                .externalId(hrEmpId)
                                .userName("hong.g@iam.com")
                                .name(UserSyncPayload.Name.builder()
                                                .familyName("Hong")
                                                .givenName("Gildong")
                                                .formatted("Hong Gildong")
                                                .build())
                                .title("Senior Engineer")
                                .active(true)
                                .extensions(Map.of("empNo", hrEmpId, "deptName", "Dev Team"))
                                .build();

                UserSyncEvent event = new UserSyncEvent("trace-123", "SAP_HR", "USER_SYNC", LocalDateTime.now(),
                                payload);

                // When
                userSyncService.processSync(event);

                // Then
                var link = identityLinkRepository.findBySystemTypeAndExternalId("SAP_HR", hrEmpId).get();
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
                var firstPayload = UserSyncPayload.builder()
                                .externalId(hrEmpId)
                                .userName("kim.f@iam.com")
                                .name(UserSyncPayload.Name.builder()
                                                .familyName("Kim")
                                                .givenName("Free")
                                                .formatted("Kim Free")
                                                .build())
                                .title("Junior")
                                .active(true)
                                .extensions(Map.of("empNo", hrEmpId, "deptName", "Dev Team"))
                                .build();
                userSyncService.processSync(
                                new UserSyncEvent("trace-1", "SAP_HR", "USER_SYNC", LocalDateTime.now(), firstPayload));

                var updatePayload = UserSyncPayload.builder()
                                .externalId(hrEmpId)
                                .userName("kim.f@iam.com")
                                .name(UserSyncPayload.Name.builder()
                                                .familyName("Kim")
                                                .givenName("Future")
                                                .formatted("Kim Future")
                                                .build())
                                .title("Senior")
                                .active(true)
                                .extensions(Map.of("empNo", hrEmpId, "deptName", "IT Team"))
                                .build();

                // When
                userSyncService.processSync(
                                new UserSyncEvent("trace-2", "SAP_HR", "USER_UPDATE", LocalDateTime.now(),
                                                updatePayload));

                // Then
                var link = identityLinkRepository.findBySystemTypeAndExternalId("SAP_HR", hrEmpId).get();
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
}