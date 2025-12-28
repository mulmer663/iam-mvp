package com.iam.connector.hr.controller;

import com.iam.connector.hr.application.service.ChangeDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrIngestController {

    private final ChangeDetectionService changeDetectionService;

    @PostMapping("/batch-sync")
    public String triggerBatchSync() {
        log.info("Triggering Batch Sync (Fingerprint Strategy)");
        changeDetectionService.detectAndSync("SAP_HR");
        return "Batch Sync Started";
    }
}
