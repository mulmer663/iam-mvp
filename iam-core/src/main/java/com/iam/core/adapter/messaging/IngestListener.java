package com.iam.core.adapter.messaging;

import com.iam.core.application.service.UserSyncService;
import com.iam.core.application.dto.UserSyncEvent;

import io.hypersistence.tsid.TSID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for raw HR data ingestion.
 * Orchestrates: Logging -> Transformation -> IAM Update.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IngestListener {

    private final UserSyncService userSyncService;

    @RabbitListener(queues = com.iam.core.config.IamRabbitConfig.INGEST_QUEUE_NAME)
    public void onRawDataIngested(@Valid UserSyncEvent message) {
        log.info("Received raw ingestion message: {}", message);

        try {
            MDC.put("traceId", message.traceId());
            MDC.put("operationType", message.eventType());
            // Delegate to Application Service
            userSyncService.processSync(message);

        } catch (Exception e) {
            log.error("Failed to adapt raw ingestion: traceId={}", message.traceId(), e);
        } finally {
            MDC.clear();
        }
    }
}
