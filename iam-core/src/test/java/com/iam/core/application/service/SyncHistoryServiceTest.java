package com.iam.core.application.service;

import com.iam.core.application.sync.SyncHistoryService;
import com.iam.core.domain.common.constant.SystemConstants;
import com.iam.core.domain.common.port.MessagePublisher;
import com.iam.core.domain.sync.SyncHistory;
import com.iam.core.domain.sync.SyncHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SyncHistoryServiceTest {

    @MockitoBean
    private MessagePublisher messagePublisher;

    @Autowired
    private SyncHistoryService syncHistoryService;

    @Autowired
    private SyncHistoryRepository syncHistoryRepository;

    @Test
    @DisplayName("성공 로그가 정상적으로 저장되어야 한다")
    void logSuccess_ShouldSaveHistory() {
        // Given
        String traceId = "test-trace-1";
        String type = "HR_SYNC";
        String target = "user1";
        Map<String, Object> resultData = Map.of("key", "value");

        // When
        syncHistoryService.logSuccess(traceId, "RECON", type, target, null, "TEST_SYSTEM", SystemConstants.SYSTEM_IAM,
                resultData, "Success message");

        // Then
        var histories = syncHistoryRepository.findByTraceId(traceId);
        assertThat(histories).hasSize(1);

        SyncHistory history = histories.get(0);
        assertThat(history.getStatus()).isEqualTo("SUCCESS");
        assertThat(history.getEventType()).isEqualTo(type);
        assertThat(history.getTargetUser()).isEqualTo(target);
        assertThat(history.getIamUserId()).isNull();
        assertThat(history.getResultData()).containsEntry("key", "value");
    }

    @Test
    @DisplayName("실패 로그가 정상적으로 저장되어야 한다")
    void logFailure_ShouldSaveHistory() {
        // Given
        String traceId = "test-trace-2";
        String type = "USER_UPDATE";
        String target = "user2";

        // When
        syncHistoryService.logFailure(traceId, "RECON", type, target, null, "TEST_SYSTEM", SystemConstants.SYSTEM_IAM,
                null, "Failure reason", 0L);

        // Then
        var histories = syncHistoryRepository.findByTraceId(traceId);
        assertThat(histories).hasSize(1);

        SyncHistory history = histories.get(0);
        assertThat(history.getStatus()).isEqualTo("FAILURE");
        assertThat(history.getMessage()).isEqualTo("Failure reason");
        assertThat(history.getRequestPayload()).isNull();
    }
}
