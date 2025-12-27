package com.iam.core.adapter.web.controller;

import com.iam.core.application.dto.HistoryResponse;
import com.iam.core.application.service.SyncHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final SyncHistoryService syncHistoryService;

    @GetMapping
    public List<HistoryResponse> getHistory(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String userId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String targetUser) {
        return syncHistoryService.getHistory(userId, targetUser);
    }
}
