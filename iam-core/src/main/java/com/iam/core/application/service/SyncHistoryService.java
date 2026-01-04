package com.iam.core.application.service;

import com.iam.core.application.dto.HistoryResponse;
import com.iam.core.domain.constant.SyncConstants;
import com.iam.core.domain.entity.SyncHistory;
import com.iam.core.domain.repository.SyncHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncHistoryService {

    private final SyncHistoryRepository syncHistoryRepository;

    /**
     * 성공 이력 기록 (최소 파라미터)
     */
    @Transactional
    public Long logSuccess(String traceId, String type, String targetUser, Long iamUserId,
                           String sourceSystem, String targetSystem, Map<String, Object> resultData, String message) {
        return logSuccess(traceId, type, targetUser, iamUserId, sourceSystem, targetSystem, resultData, message, null, null, null, 0L);
    }

    /**
     * 성공 이력 기록 (상세 파라미터)
     */
    @Transactional
    public Long logSuccess(String traceId, String type, String targetUser, Long iamUserId,
                           String sourceSystem, String targetSystem, Map<String, Object> resultData,
                           String message, Long parentId, Long duration, Map<String, Object> requestPayload, Long revId) {

        return saveHistory(traceId, type, SyncConstants.STATUS_SUCCESS, targetUser, iamUserId,
                sourceSystem, targetSystem, resultData, message, parentId, duration, requestPayload, revId);
    }

    /**
     * 실패 이력 기록
     */
    @Transactional
    public Long logFailure(String traceId, String type, String targetUser, Long iamUserId,
                           String sourceSystem, String targetSystem, Map<String, Object> requestPayload,
                           String errorDetails, long revId) {
        return saveHistory(traceId, type, SyncConstants.STATUS_FAILURE, targetUser, iamUserId,
                sourceSystem, targetSystem, null, errorDetails, null, null, requestPayload, revId);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> getHistory(String userId, String targetUser) {
        // ... (조회 로직은 기존과 동일하되 가독성을 위해 유지)
        return fetchHistories(userId, targetUser).stream()
                .map(this::toResponse)
                .toList();
    }

    private List<SyncHistory> fetchHistories(String userId, String targetUser) {
        boolean hasUserId = userId != null && !userId.isBlank();
        boolean hasTargetUser = targetUser != null && !targetUser.isBlank();

        if (hasUserId && hasTargetUser) {
            try {
                return syncHistoryRepository.findByIamUserIdOrTargetUser(Long.parseLong(userId), targetUser);
            } catch (NumberFormatException e) {
                return syncHistoryRepository.findByTargetUser(targetUser);
            }
        } else if (hasUserId) {
            try {
                return syncHistoryRepository.findByIamUserId(Long.parseLong(userId));
            } catch (NumberFormatException e) {
                return List.of();
            }
        } else if (hasTargetUser) {
            return syncHistoryRepository.findByTargetUser(targetUser);
        }
        return syncHistoryRepository.findAllByOrderByCreatedAtDesc();
    }

    private Long saveHistory(String traceId, String type, String status, String targetUser, Long iamUserId,
                             String sourceSystem, String targetSystem, Map<String, Object> resultData, String message,
                             Long parentHistoryId, Long durationMs, Map<String, Object> requestPayload, Long revId) {
        try {
            var history = SyncHistory.builder()
                    .traceId(traceId)
                    .type(type)
                    .status(status)
                    .targetUser(targetUser)
                    .iamUserId(iamUserId)
                    .sourceSystem(sourceSystem)
                    .targetSystem(targetSystem)
                    .message(message)
                    .resultData(resultData)
                    .parentHistoryId(parentHistoryId)
                    .durationMs(durationMs)
                    .requestPayload(requestPayload)
                    .revId(revId != null ? revId : 0L) // rev_id가 nullable=false이므로 기본값 처리
                    .completedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();

            var saved = syncHistoryRepository.save(history);
            log.info("Saved sync history [{}]: {} - {} (ID: {})", status, type, traceId, saved.getId());
            return saved.getId();
        } catch (Exception e) {
            log.error("Failed to save sync history", e);
            return null;
        }
    }

    private HistoryResponse toResponse(SyncHistory history) {
        return new HistoryResponse(
                String.valueOf(history.getId()),
                history.getTraceId(),
                history.getType(),
                history.getStatus(),
                history.getTargetUser(),
                history.getSourceSystem(),
                history.getTargetSystem(),
                history.getCreatedAt(),
                history.getMessage(),
                history.getResultData(),
                history.getRequestPayload(),
                history.getParentHistoryId(),
                history.getDurationMs(),
                history.getRevId());
    }
}