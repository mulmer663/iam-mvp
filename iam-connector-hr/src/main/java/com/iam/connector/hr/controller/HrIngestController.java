package com.iam.connector.hr.controller;

import com.iam.connector.hr.application.service.HrSyncService;
import com.iam.connector.hr.application.service.ChangeDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrIngestController {

    private final HrSyncService hrSyncService;
    private final ChangeDetectionService changeDetectionService;

    @PostMapping("/sync")
    public String receiveHrData(@RequestBody Map<String, Object> payload) {
        log.info("Received HR data via HTTP (Single): {}", payload);
        hrSyncService.syncUser("SAP_HR", payload);
        return "Sync Event Published";
    }

    @PostMapping("/full-sync")
    public String triggerFullSync() {
        log.info("Triggering full sync via HTTP (Mock Data Only)");
        hrSyncService.fullSync("SAP_HR");
        return "Full Sync Started";
    }

    @PostMapping("/batch-sync")
    public String triggerBatchSync() {
        log.info("Triggering Batch Sync (Fingerprint Strategy)");
        changeDetectionService.detectAndSync("SAP_HR");
        return "Batch Sync Started";
    }
}
