package com.iam.core.adapter.web.controller.sync;

import com.iam.core.application.common.HistoryResponse;
import com.iam.core.application.sync.SyncHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
