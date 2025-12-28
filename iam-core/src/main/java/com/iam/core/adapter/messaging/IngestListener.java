package com.iam.core.adapter.messaging;

import com.iam.core.application.service.UserSyncService;
import com.iam.core.application.dto.UserSyncEvent;
import com.iam.core.application.dto.UserSyncPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
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
    public void onRawDataIngested(Map<String, Object> message) {
        log.info("Received raw ingestion message: {}", message);

        String traceId = (String) message.getOrDefault("traceId", UUID.randomUUID().toString());
        String systemId = (String) message.get("systemId");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rawPayload = (Map<String, Object>) message.get("payload");
            if (rawPayload == null) {
                log.error("Missing payload in ingestion message: {}", traceId);
                return;
            }

            // Convert raw map to DTO
            UserSyncPayload payload = mapToPayload(rawPayload);
            UserSyncEvent event = new UserSyncEvent(traceId, systemId, "USER_SYNC", LocalDateTime.now(), payload);

            // Delegate to Application Service
            userSyncService.processSync(event);

        } catch (Exception e) {
            log.error("Failed to adapt raw ingestion: traceId={}", traceId, e);
        }
    }

    private UserSyncPayload mapToPayload(Map<String, Object> map) {
        UserSyncPayload payload = new UserSyncPayload();
        payload.setExternalId((String) map.get("externalId"));
        payload.setUserName((String) map.get("userName"));
        payload.setTitle((String) map.get("title"));
        payload.setActive(map.get("active") instanceof Boolean b ? b : true);

        // Name
        UserSyncPayload.Name name = new UserSyncPayload.Name();
        name.setFamilyName((String) map.get("familyName"));
        name.setGivenName((String) map.get("givenName"));
        name.setFormatted((String) map.get("formattedName"));
        payload.setName(name);

        // All others as extensions
        map.forEach((k, v) -> {
            if (!isCoreField(k)) {
                payload.addExtension(k, v);
            }
        });

        return payload;
    }

    private boolean isCoreField(String key) {
        return List.of("externalId", "userName", "title", "active", "familyName", "givenName", "formattedName")
                .contains(key);
    }
}
