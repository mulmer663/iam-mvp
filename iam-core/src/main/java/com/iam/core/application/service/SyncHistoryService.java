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
    public Long logSuccess(String traceId, String type, String targetUser, String sourceSystem, Object payload,
            String responsePayload) {
        return logSuccess(traceId, type, targetUser, sourceSystem, payload, responsePayload, null, null);
    }

    @Transactional
    public Long logSuccess(String traceId, String type, String targetUser, String sourceSystem, Object payload,
            String responsePayload, Long parentId, Long duration) {
        return saveHistory(traceId, type, SyncConstants.STATUS_SUCCESS, targetUser, sourceSystem, null, payload,
                responsePayload, parentId,
                duration);
    }

    @Transactional
    public void logFailure(String traceId, String type, String targetUser, String sourceSystem, Object payload,
            String errorDetails) {
        saveHistory(traceId, type, SyncConstants.STATUS_FAILURE, targetUser, sourceSystem, null, payload, errorDetails,
                null, null);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> getHistory(String userId, String targetUser) {
        List<SyncHistory> list;
        if (targetUser != null && !targetUser.isBlank()) {
            list = syncHistoryRepository.findByTargetUser(targetUser);
        } else {
            list = syncHistoryRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream()
                .map(this::toResponse)
                .toList();
    }

    private Long saveHistory(String traceId, String type, String status, String targetUser, String sourceSystem,
            String targetSystem, Object requestPayload, String responsePayload, Long parentHistoryId,
            Long durationMs) {
        try {
            var history = SyncHistory.builder()
                    .traceId(traceId)
                    .type(type)
                    .status(status)
                    .targetUser(targetUser)
                    .sourceSystem(sourceSystem)
                    .targetSystem(targetSystem)
                    .message(responsePayload)
                    .responsePayload(responsePayload)
                    .parentHistoryId(parentHistoryId)
                    .durationMs(durationMs)
                    .completedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();

            if (requestPayload != null) {
                Object unwrapped = unwrap(requestPayload);
                String json = objectMapper.writeValueAsString(unwrapped);
                history.setRequestPayload(json);
                history.setPayload(json); // Sync with 'payload' column for CSV
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
                history.getCreatedAt(),
                history.getMessage(),
                history.getPayload(),
                history.getParentHistoryId(),
                history.getDurationMs());
    }
}
