package com.iam.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.core.application.dto.HistoryResponse;
import com.iam.core.domain.entity.SyncHistory;
import com.iam.core.domain.repository.SyncHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncHistoryService {

    private final SyncHistoryRepository syncHistoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logSuccess(String traceId, String type, String targetUser, String sourceSystem, Object payload,
            String responsePayload) {
        saveHistory(traceId, type, "SUCCESS", targetUser, sourceSystem, null, payload, responsePayload);
    }

    @Transactional
    public void logFailure(String traceId, String type, String targetUser, String sourceSystem, Object payload,
            String errorDetails) {
        saveHistory(traceId, type, "FAILURE", targetUser, sourceSystem, null, payload, errorDetails);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> getAllHistory() {
        return syncHistoryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private void saveHistory(String traceId, String type, String status, String targetUser, String sourceSystem,
            String targetSystem, Object requestPayload, String responsePayload) {
        try {
            var history = SyncHistory.builder()
                    .traceId(traceId)
                    .type(type)
                    .status(status)
                    .targetUser(targetUser)
                    .sourceSystem(sourceSystem)
                    .targetSystem(targetSystem)
                    .responsePayload(responsePayload)
                    .createdAt(LocalDateTime.now())
                    .build();

            if (requestPayload != null) {
                history.setRequestPayload(objectMapper.writeValueAsString(requestPayload));
            }

            syncHistoryRepository.save(history);
            log.info("Saved sync history [{}]: {} - {}", status, type, traceId);
        } catch (Exception e) {
            log.error("Failed to save sync history", e);
        }
    }

    private HistoryResponse toResponse(SyncHistory history) {
        return new HistoryResponse(
                String.valueOf(history.getId()),
                history.getTraceId(),
                history.getType(),
                history.getStatus(),
                history.getTargetUser(),
                history.getCreatedAt(),
                "", // Legacy message field
                history.getRequestPayload());
    }
}
