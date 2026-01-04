package com.iam.core.adapter.web.controller;

import com.iam.core.application.dto.HistoryResponse;
import com.iam.core.application.service.SyncHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final SyncHistoryService syncHistoryService;

    @GetMapping
    public Page<HistoryResponse> getHistory(
            @RequestParam(required = false) Long userId, // String 대신 Long으로 직접 선언
            @RequestParam(required = false) String targetUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return syncHistoryService.getHistory(userId, targetUser, pageable);
    }
}
