package com.iam.core.adapter.messaging;

import com.iam.core.application.service.IdentityCorrelationService;
import com.iam.core.application.service.IamUserUpdateService;
import com.iam.core.application.service.SyncHistoryService;
import com.iam.core.application.service.TransformationService;
import com.iam.core.domain.entity.IamUser;
import com.iam.core.domain.entity.IdentityLink;
import com.iam.core.domain.repository.IdentityLinkRepository;
import com.iam.core.domain.vo.UniversalData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listener for raw HR data ingestion.
 * Orchestrates: Logging -> Transformation -> IAM Update.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IngestListener {

    private final TransformationService transformationService;
    private final IdentityCorrelationService correlationService;
    private final IamUserUpdateService userUpdateService;
    private final IdentityLinkRepository identityLinkRepository;
    private final SyncHistoryService syncHistoryService;

    @RabbitListener(queues = com.iam.core.config.IamRabbitConfig.INGEST_QUEUE_NAME)
    public void onRawDataIngested(Map<String, Object> event) {
        String traceId = "UNKNOWN";
        String systemId = "UNKNOWN";
        Map<String, Object> payload = null;

        try {
            traceId = (String) event.get("traceId");
            systemId = (String) event.get("systemId");
            @SuppressWarnings("unchecked")
            Map<String, Object> rawPayload = (Map<String, Object>) event.get("payload");
            payload = rawPayload;

            log.info("Processing ingestion event: traceId={}, systemId={}", traceId, systemId);

            // 1. Initial Logging
            syncHistoryService.logSuccess(traceId, "RAW_INGEST", "SYSTEM", systemId, payload, "Raw data received");

            // 2. Transformation
            Map<String, UniversalData> transformedData = transformationService.transform(systemId, payload);
            syncHistoryService.logSuccess(traceId, "TRANSFORM", "SYSTEM", systemId, transformedData,
                    "Transformation completed");

            // 3. Identity Correlation
            // Fallback: Use empNo if externalId is missing (common in HR connector)
            String externalId = (String) payload.getOrDefault("externalId", payload.get("empNo"));
            if (externalId == null)
                throw new RuntimeException("Missing externalId or empNo in payload");

            var userOpt = correlationService.correlate(systemId, externalId);

            // 4. IAM Object Update
            IamUser user;
            if (userOpt.isPresent()) {
                user = userUpdateService.update(userOpt.get(), transformedData);
                log.info("Updated existing user: traceId={}, userId={}", traceId, user.getId());
            } else {
                user = userUpdateService.create(externalId, transformedData);

                // Create IdentityLink if performing a create
                IdentityLink link = new IdentityLink();
                link.setIamUserId(user.getId());
                link.setSystemType(systemId);
                link.setExternalId(externalId);
                link.setActive(true);
                identityLinkRepository.save(link);

                log.info("Created new user and identity link: traceId={}, userId={}", traceId, user.getId());
            }

            // 5. Success Logging
            syncHistoryService.logSuccess(traceId, "IAM_UPDATE", user.getUserName(), systemId, transformedData,
                    "IAM User updated successfully");

        } catch (Exception e) {
            log.error("Failed to process raw ingestion: traceId={}", traceId, e);
            syncHistoryService.logFailure(
                    traceId,
                    "INGEST_FAILURE",
                    "SYSTEM",
                    systemId,
                    payload,
                    "Processing failed: " + e.getMessage());
            // TODO: Log to SyncTransformFailure table specifically if it's a rule error
        }
    }
}
