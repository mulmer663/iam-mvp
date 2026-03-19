package com.iam.adapter.db.interfaces.rest;

import com.iam.adapter.db.application.DynamicDbService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/api/v1/sync")
@RequiredArgsConstructor
@Slf4j
public class DbAdapterController {

    private final DynamicDbService dynamicDbService;
    private final RabbitTemplate rabbitTemplate;

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerSync(@RequestBody DbConnectionRequest request) {
        log.info("Triggering sync for UI requested dynamic database connection");

        // Fetch raw data dynamically
        List<Map<String, Object>> rawDataList = dynamicDbService.fetchSourceData(
                request.getDriver(),
                request.getUrl(),
                request.getUsername(),
                request.getPassword(),
                request.getQuery());

        int count = 0;
        // Send strictly raw data to Engine via RabbitMQ (No transformation)
        for (Map<String, Object> rawData : rawDataList) {
            rabbitTemplate.convertAndSend("RAW_INBOUND_DATA", rawData);
            count++;
        }

        return ResponseEntity.ok("Successfully sent " + count + " raw records to Engine.");
    }

    @Data
    public static class DbConnectionRequest {
        private String driver;
        private String url;
        private String username;
        private String password;
        private String query;
    }
}
