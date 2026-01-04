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

    @Transactional
    public Long logSuccess(String traceId, String type, String targetUser, Long iamUserId, String sourceSystem,
            Map<String, Object> resultData,
            String message) {
        return logSuccess(traceId, type, targetUser, iamUserId, sourceSystem, resultData, message, null, null, null,
                null);
    }

    @Transactional
    public Long logSuccess(String traceId, String type, String targetUser, Long iamUserId, String sourceSystem,
            Map<String, Object> resultData,
            String message, Long parentId, Long duration, Map<String, Object> requestPayload,
            List<Long> appliedRules) {
        return saveHistory(traceId, type, SyncConstants.STATUS_SUCCESS, targetUser, iamUserId, sourceSystem, null,
                resultData,
                message, parentId,
                duration, requestPayload, appliedRules);
    }

    @Transactional
    public void logFailure(String traceId, String type, String targetUser, Long iamUserId, String sourceSystem,
            Map<String, Object> requestPayload,
            String errorDetails) {
        saveHistory(traceId, type, SyncConstants.STATUS_FAILURE, targetUser, iamUserId, sourceSystem, null, null,
                errorDetails,
                null, null, requestPayload, null);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> getHistory(String userId, String targetUser) {
        List<SyncHistory> list;
        boolean hasUserId = userId != null && !userId.isBlank();
        boolean hasTargetUser = targetUser != null && !targetUser.isBlank();

        if (hasUserId && hasTargetUser) {
            try {
                Long iamUserId = Long.parseLong(userId);
                list = syncHistoryRepository.findByIamUserIdOrTargetUser(iamUserId, targetUser);
            } catch (NumberFormatException e) {
                log.warn("Invalid userId format, falling back to targetUser: {}", userId);
                list = syncHistoryRepository.findByTargetUser(targetUser);
            }
        } else if (hasUserId) {
            try {
                Long iamUserId = Long.parseLong(userId);
                list = syncHistoryRepository.findByIamUserId(iamUserId);
            } catch (NumberFormatException e) {
                log.warn("Invalid userId format: {}", userId);
                list = List.of();
            }
        } else if (hasTargetUser) {
            list = syncHistoryRepository.findByTargetUser(targetUser);
        } else {
            list = syncHistoryRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream()
                .map(this::toResponse)
                .toList();
    }

    private Long saveHistory(String traceId, String type, String status, String targetUser, Long iamUserId,
            String sourceSystem,
            String targetSystem, Map<String, Object> resultData, String message, Long parentHistoryId,
            Long durationMs, Map<String, Object> requestPayload, List<Long> appliedRules) {
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
                    .appliedRules(appliedRules)
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
                history.getAppliedRules());
    }
}
