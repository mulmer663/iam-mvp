package com.iam.connector.hr.application.service;

import com.iam.connector.hr.application.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service to handle HR synchronization.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HrSyncService {

    private final EventPublisher eventPublisher;

    /**
     * Synchronizes a single user from HR.
     */
    public void syncUser(String systemId, Map<String, Object> rawData) {
        log.info("Processing single user sync for system: {}", systemId);
        eventPublisher.publish(systemId, rawData);
    }

    /**
     * Simulates fetching and synchronizing all users from an HR system.
     */
    public void fullSync(String systemId) {
        log.info("Starting full sync for system: {}", systemId);

        // Simulating data fetch from HR System
        List<Map<String, Object>> mockUsers = List.of(
                Map.of("externalId", "2023001", "firstName", "Gildong", "lastName", "Hong", "position", "Manager",
                        "empNo", "2023001"),
                Map.of("externalId", "2023002", "firstName", "Chulsoo", "lastName", "Kim", "position", "Engineer",
                        "empNo", "2023002"),
                Map.of("externalId", "2023003", "firstName", "Younghee", "lastName", "Lee", "position", "Designer",
                        "empNo", "2023003"));

        mockUsers.forEach(user -> eventPublisher.publish(systemId, user));
        log.info("Finished publishing {} users for full sync", mockUsers.size());
    }
}
