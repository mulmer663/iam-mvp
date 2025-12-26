package com.iam.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.core.application.dto.HistoryResponse;
import com.iam.core.domain.entity.SyncHistory;
import com.iam.core.domain.repository.SyncHistoryRepository;
import io.hypersistence.tsid.TSID;
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
    public void logSuccess(String traceId, String type, String targetUser, Object payload, String message) {
        saveHistory(traceId, type, "SUCCESS", targetUser, payload, message);
    }

    @Transactional
    public void logFailure(String traceId, String type, String targetUser, Object payload, String message) {
        saveHistory(traceId, type, "FAILURE", targetUser, payload, message);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> getAllHistory() {
        return syncHistoryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private void saveHistory(String traceId, String type, String status, String targetUser, Object payload,
            String message) {
        try {
            var history = new SyncHistory();
            history.setId(TSID.fast().toLong());
            history.setTraceId(traceId);
            history.setType(type);
            history.setStatus(status);
            history.setTargetUser(targetUser);
            history.setMessage(message);
            history.setCreatedAt(LocalDateTime.now());

            if (payload != null) {
                history.setPayload(objectMapper.writeValueAsString(payload));
            }

            syncHistoryRepository.save(history);
            log.info("Saved sync history [{}]: {} - {}", status, type, traceId);
        } catch (Exception e) {
            log.error("Failed to save sync history", e);
            // Audit log failure shouldn't stop the main flow, but we log it.
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
                history.getMessage(),
                history.getPayload());
    }
}
