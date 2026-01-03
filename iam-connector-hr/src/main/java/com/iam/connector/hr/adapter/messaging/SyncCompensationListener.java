package com.iam.connector.hr.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.connector.hr.application.port.out.SnapshotPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Listener for Sync Compensation Events.
 * Triggers rollback (snapshot deletion) when a sync failure occurs in Core.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncCompensationListener {

    private final SnapshotPort snapshotPort;
    private final ObjectMapper objectMapper;

    // Must match iam-core event structure
    record SyncCompensationEvent(
            String traceId,
            String systemId,
            String externalId,
            String reason,
            LocalDateTime timestamp) {
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "hr.queue.compensation", durable = "true"), exchange = @Exchange(value = "iam.topic", type = "topic"), key = "iam.event.compensation"))
    public void onCompensationEvent(Map<String, Object> payload) {
        try {
            SyncCompensationEvent event = objectMapper.convertValue(payload, SyncCompensationEvent.class);
            log.warn("🚨 Compensation requested for System: {}, ExternalId: {}. Reason: {}",
                    event.systemId(), event.externalId(), event.reason());

            // 1. Revert Snapshot to previous state safely
            snapshotPort.revert(event.externalId());
            log.info("✅ Snapshot reverted for {}. Re-sync enabled.", event.externalId());

        } catch (Exception e) {
            log.error("Failed to process compensation event", e);
        }
    }
}
