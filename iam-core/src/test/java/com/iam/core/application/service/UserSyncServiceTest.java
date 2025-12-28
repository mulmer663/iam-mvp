package com.iam.core.application.service;

import com.iam.core.domain.constant.AttributeConstants;
import com.iam.core.domain.constant.SyncConstants;
import com.iam.core.domain.entity.EnterpriseUserExtension;
import com.iam.core.domain.port.MessagePublisher;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.IdentityLinkRepository;
import com.iam.core.application.dto.UserSyncEvent;

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
                Map<String, Object> payload = Map.of(
                                AttributeConstants.EXTERNAL_ID, hrEmpId,
                                AttributeConstants.USERNAME, "hong.g@iam.com",
                                AttributeConstants.FAMILY_NAME, "Hong",
                                AttributeConstants.GIVEN_NAME, "Gildong",
                                AttributeConstants.FORMATTED_NAME, "Hong Gildong",
                                AttributeConstants.TITLE, "Senior Engineer",
                                AttributeConstants.ACTIVE, true,
                                AttributeConstants.EMP_NO, hrEmpId,
                                AttributeConstants.DEPT_NAME, "Dev Team");

                UserSyncEvent event = new UserSyncEvent("trace-123", "SAP_HR", SyncConstants.EVENT_USER_SYNC,
                                LocalDateTime.now(),
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
                Map<String, Object> firstPayload = Map.of(
                                AttributeConstants.EXTERNAL_ID, hrEmpId,
                                AttributeConstants.USERNAME, "kim.f@iam.com",
                                AttributeConstants.FAMILY_NAME, "Kim",
                                AttributeConstants.GIVEN_NAME, "Free",
                                AttributeConstants.FORMATTED_NAME, "Kim Free",
                                AttributeConstants.TITLE, "Junior",
                                AttributeConstants.ACTIVE, true,
                                AttributeConstants.EMP_NO, hrEmpId,
                                AttributeConstants.DEPT_NAME, "Dev Team");

                userSyncService.processSync(
                                new UserSyncEvent("trace-1", "SAP_HR", SyncConstants.EVENT_USER_SYNC,
                                                LocalDateTime.now(), firstPayload));

                Map<String, Object> updatePayload = Map.of(
                                AttributeConstants.EXTERNAL_ID, hrEmpId,
                                AttributeConstants.USERNAME, "kim.f@iam.com",
                                AttributeConstants.FAMILY_NAME, "Kim",
                                AttributeConstants.GIVEN_NAME, "Future",
                                AttributeConstants.FORMATTED_NAME, "Kim Future",
                                AttributeConstants.TITLE, "Senior",
                                AttributeConstants.ACTIVE, true,
                                AttributeConstants.EMP_NO, hrEmpId,
                                AttributeConstants.DEPT_NAME, "IT Team");

                // When
                userSyncService.processSync(
                                new UserSyncEvent("trace-2", "SAP_HR", SyncConstants.EVENT_USER_UPDATE,
                                                LocalDateTime.now(),
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