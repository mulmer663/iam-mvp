package com.iam.core.service;

import com.iam.core.domain.entity.UserStatus;
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
    @DisplayName("HR 신규 사용자 동기화 시 DB에 정상 저장되고 TSID가 생성되어야 한다")
    void processHrSync_NewUser_ShouldSaveWithTsid() {
        // Given
        String hrEmpId = "2024001";
        UserSyncEvent event = new UserSyncEvent(
                "trace-123",
                "SYNC_USER",
                LocalDateTime.now(),
                new UserSyncPayload(
                        hrEmpId,
                        "홍길동",
                        Map.of("deptCode", "DEV01", "position", "Senior")));

        // When
        userSyncService.processHrSync(event);

        // Then
        // 1. IdentityLink 검증
        var link = identityLinkRepository.findBySystemTypeAndExternalId("HR", hrEmpId)
                .orElseThrow(() -> new AssertionError("IdentityLink not found"));

        Long iamUserId = link.getIamUserId();
        assertThat(iamUserId).isNotNull();

        // 2. IamUser 검증
        var user = iamUserRepository.findById(iamUserId)
                .orElseThrow(() -> new AssertionError("IamUser not found"));

        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getId()).isEqualTo(iamUserId); // TSID match

        // 3. IamUserExtension 검증
        assertThat(user.getExtension()).isNotNull();
        assertThat(user.getExtension().getAttributes().get("deptCode")).isEqualTo("DEV01");
    }

    @Test
    @DisplayName("기존 사용자 정보 변경 시 DB 데이터가 업데이트 되어야 한다")
    void processHrSync_UpdateUser_ShouldUpdateData() {
        // Given
        // 1. 초기 데이터 생성
        String hrEmpId = "2024002";
        UserSyncEvent firstEvent = new UserSyncEvent(
                "trace-1",
                "SYNC_USER",
                LocalDateTime.now(),
                new UserSyncPayload(hrEmpId, "김철수", Map.of("dept", "HR")));
        userSyncService.processHrSync(firstEvent);

        // 2. 변경 데이터 준비
        UserSyncEvent updateEvent = new UserSyncEvent(
                "trace-2",
                "SYNC_USER",
                LocalDateTime.now(),
                new UserSyncPayload(hrEmpId, "김영희", Map.of("dept", "IT")));

        // When
        userSyncService.processHrSync(updateEvent);

        // Then
        var link = identityLinkRepository.findBySystemTypeAndExternalId("HR", hrEmpId).get();
        var user = iamUserRepository.findById(link.getIamUserId()).get();

        assertThat(user.getName()).isEqualTo("김영희");
        assertThat(user.getExtension().getAttributes().get("dept")).isEqualTo("IT");
    }
}
