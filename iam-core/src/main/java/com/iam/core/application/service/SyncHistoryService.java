package com.iam.core.application.service;

import com.iam.core.application.dto.HistoryResponse;
import com.iam.core.domain.constant.SyncConstants;
import com.iam.core.domain.entity.SyncHistory;
import com.iam.core.domain.repository.SyncHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public Long logSuccess(String traceId, String syncDirection, String eventType, String targetUser, Long iamUserId,
            String sourceSystem, String targetSystem, Map<String, Object> resultData, String message) {
        return logSuccess(traceId, syncDirection, eventType, targetUser, iamUserId, sourceSystem, targetSystem,
                resultData,
                message, null, null, 0L, 0L);
    }

    /**
     * 성공 이력 기록 (상세 파라미터)
     */
    @Transactional
    public Long logSuccess(String traceId, String syncDirection, String eventType, String targetUser, Long iamUserId,
            String sourceSystem, String targetSystem, Map<String, Object> resultData,
            String message, Long parentId,
            Map<String, Object> requestPayload,
            long userRevId, long ruleRevId) {

        return saveHistory(traceId, syncDirection, eventType, SyncConstants.STATUS_SUCCESS, targetUser, iamUserId,
                sourceSystem, targetSystem, resultData, message, parentId, requestPayload, userRevId, ruleRevId);
    }

    /**
     * 실패 이력 기록
     */
    @Transactional
    public Long logFailure(String traceId, String syncDirection, String eventType, String targetUser, Long iamUserId,
            String sourceSystem, String targetSystem, Map<String, Object> requestPayload,
            String errorDetails, long ruleRevId) {
        return saveHistory(traceId, syncDirection, eventType, SyncConstants.STATUS_FAILURE, targetUser, iamUserId,
                sourceSystem, targetSystem, null, errorDetails, null, requestPayload, 0L, ruleRevId);
    }

    @Transactional(readOnly = true)
    public Page<HistoryResponse> getHistory(Long iamUserId, String targetUser, Pageable pageable) {
        // 1. 다이나믹 쿼리를 위한 로직 (QueryDSL이 없다면 간단한 조건별 호출로 정리)
        Page<SyncHistory> histories = fetchHistories(iamUserId, targetUser, pageable);

        return histories.map(this::toResponse);
    }

    private Page<SyncHistory> fetchHistories(Long iamUserId, String targetUser, Pageable pageable) {
        if (iamUserId != null && targetUser != null) {
            return syncHistoryRepository.findByIamUserIdOrTargetUser(iamUserId, targetUser, pageable);
        } else if (iamUserId != null) {
            return syncHistoryRepository.findByIamUserId(iamUserId, pageable);
        } else if (targetUser != null && !targetUser.isBlank()) {
            return syncHistoryRepository.findByTargetUser(targetUser, pageable);
        }
        return syncHistoryRepository.findAll(pageable); // 최신순 정렬은 Pageable에서 처리
    }

    private Long saveHistory(String traceId, String syncDirection, String eventType, String status, String targetUser,
            Long iamUserId,
            String sourceSystem, String targetSystem, Map<String, Object> resultData, String message,
            Long parentHistoryId, Map<String, Object> requestPayload, Long userRevId, Long ruleRevId) {
        try {
            var history = SyncHistory.builder()
                    .traceId(traceId)
                    .syncDirection(syncDirection)
                    .eventType(eventType)
                    .status(status)
                    .targetUser(targetUser)
                    .iamUserId(iamUserId)
                    .sourceSystem(sourceSystem)
                    .targetSystem(targetSystem)
                    .message(message)
                    .resultData(resultData)
                    .parentHistoryId(parentHistoryId)
                    .requestPayload(requestPayload)
                    .userRevId(userRevId != null ? userRevId : 0L)
                    .ruleRevId(ruleRevId != null ? ruleRevId : 0L)
                    .expiresAt(LocalDateTime.now().plusDays(90))
                    .build();

            var saved = syncHistoryRepository.save(history);
            log.info("Saved sync history [{}]: {} - {} (ID: {})", status, eventType, traceId, saved.getId());
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
                history.getEventType(),
                history.getStatus(),
                history.getTargetUser(),
                history.getSourceSystem(),
                history.getTargetSystem(),
                history.getSyncDirection(),
                history.getCreatedAt(),
                history.getMessage(),
                history.getResultData(),
                history.getRequestPayload(),
                history.getParentHistoryId(),
                history.getUserRevId(),
                history.getRuleRevId());
    }
}