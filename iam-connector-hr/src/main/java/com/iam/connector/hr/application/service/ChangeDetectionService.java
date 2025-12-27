package com.iam.connector.hr.application.service;

import com.iam.connector.hr.application.port.out.EventPublisher;
import com.iam.connector.hr.application.port.out.HrSourcePort;
import com.iam.connector.hr.application.port.out.SnapshotPort;
import com.iam.connector.hr.domain.model.HrRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service to detect changes in HR data and synchronize with IAM.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChangeDetectionService {

    private final HrSourcePort hrSourcePort;
    private final SnapshotPort snapshotPort;
    private final EventPublisher eventPublisher;

    @Transactional
    public void detectAndSync(String systemId) {
        log.info("Starting Batch sync for system: {}", systemId);

        // 1. Fetch current data from source
        List<HrRecord> currentRecords = hrSourcePort.fetchAll();
        Set<String> currentIds = new HashSet<>();

        int addCount = 0;
        int updateCount = 0;

        // 2. Compare with snapshots
        for (HrRecord record : currentRecords) {
            String externalId = record.externalId();
            currentIds.add(externalId);

            var existingHashOpt = snapshotPort.getHash(externalId);

            if (existingHashOpt.isEmpty()) {
                // New record
                log.info("New record detected: {}", externalId);
                eventPublisher.publish(systemId, record.data());
                snapshotPort.save(externalId, record.hash(), systemId);
                addCount++;
            } else if (!existingHashOpt.get().equals(record.hash())) {
                // Modified record
                log.info("Modified record detected: {}", externalId);
                eventPublisher.publish(systemId, record.data());
                snapshotPort.save(externalId, record.hash(), systemId);
                updateCount++;
            } else {
                log.debug("No change for record: {}", externalId);
            }
        }

        // 3. Reconciliation (Delete detection)
        Set<String> snapshotIds = snapshotPort.getAllExternalIds(systemId);
        snapshotIds.removeAll(currentIds);

        int deleteCount = 0;
        for (String deletedId : snapshotIds) {
            log.warn("Record deletion detected: {}", deletedId);

            // Send event with status 'TERMINATED' or similar if needed.
            // For now, we follow the same event format but perhaps with a status flag or
            // specialized eventType.
            // AGENTS.md says "Reconciliation: Treat as DELETE/TERMINATION event"

            // Simple approach: send an empty or status=INACTIVE payload
            eventPublisher.publish(systemId,
                    java.util.Map.of("externalId", deletedId, "status", "INACTIVE", "empNo", deletedId));

            snapshotPort.delete(deletedId);
            deleteCount++;
        }

        log.info("Batch sync finished: systemId={}, ADD={}, UPDATE={}, DELETE={}", systemId, addCount, updateCount,
                deleteCount);
    }
}
