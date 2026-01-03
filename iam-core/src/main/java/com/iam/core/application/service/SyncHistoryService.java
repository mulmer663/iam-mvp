package com.iam.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Transactional
    public Long logSuccess(String traceId, String type, String targetUser, Long iamUserId, String sourceSystem,
            Object payload,
            String responsePayload) {
        return logSuccess(traceId, type, targetUser, iamUserId, sourceSystem, payload, responsePayload, null, null,
                null);
    }

    @Transactional
    public Long logSuccess(String traceId, String type, String targetUser, Long iamUserId, String sourceSystem,
            Object payload,
            String responsePayload, Long parentId, Long duration, Object requestPayload) {
        return saveHistory(traceId, type, SyncConstants.STATUS_SUCCESS, targetUser, iamUserId, sourceSystem, null,
                payload,
                responsePayload, parentId,
                duration, requestPayload);
    }

    @Transactional
    public void logFailure(String traceId, String type, String targetUser, Long iamUserId, String sourceSystem,
            Object requestPayload,
            String errorDetails) {
        saveHistory(traceId, type, SyncConstants.STATUS_FAILURE, targetUser, iamUserId, sourceSystem, null, null,
                errorDetails,
                null, null, requestPayload);
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
            String targetSystem, Object detailPayload, String responsePayload, Long parentHistoryId,
            Long durationMs, Object originalRequestPayload) {
        try {
            var history = SyncHistory.builder()
                    .traceId(traceId)
                    .type(type)
                    .status(status)
                    .targetUser(targetUser)
                    .iamUserId(iamUserId)
                    .sourceSystem(sourceSystem)
                    .targetSystem(targetSystem)
                    .message(responsePayload)
                    .responsePayload(responsePayload)
                    .parentHistoryId(parentHistoryId)
                    .durationMs(durationMs)
                    .completedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();

            if (detailPayload != null) {
                Object unwrapped = unwrap(detailPayload);
                String json = objectMapper.writeValueAsString(unwrapped);
                history.setPayload(json);
            }

            if (originalRequestPayload != null) {
                Object unwrapped = unwrap(originalRequestPayload);
                String json = objectMapper.writeValueAsString(unwrapped);
                history.setRequestPayload(json);
            }

            var saved = syncHistoryRepository.save(history);
            log.info("Saved sync history [{}]: {} - {} (ID: {})", status, type, traceId, saved.getId());
            return saved.getId();
        } catch (Exception e) {
            log.error("Failed to save sync history", e);
            return null;
        }
    }

    private Object unwrap(Object payload) {
        if (payload == null)
            return null;

        if (payload instanceof com.iam.core.domain.vo.UniversalData ud) {
            return ud.getValue();
        } else if (payload instanceof Map<?, ?> map) {
            java.util.Map<Object, Object> unwrapped = new java.util.HashMap<>();
            map.forEach((k, v) -> unwrapped.put(k, unwrap(v)));
            return unwrapped;
        } else if (payload instanceof Iterable<?> it) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            it.forEach(i -> list.add(unwrap(i)));
            return list;
        }
        return payload;
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
                history.getPayload(),
                history.getRequestPayload(),
                history.getParentHistoryId(),
                history.getDurationMs());
    }
}
