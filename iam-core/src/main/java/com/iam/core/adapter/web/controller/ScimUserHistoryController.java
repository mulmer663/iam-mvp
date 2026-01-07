package com.iam.core.adapter.web.controller;

import com.iam.core.application.dto.ScimUserResponse;
import com.iam.core.application.dto.UserRevisionResponse;
import com.iam.core.application.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history/users")
@RequiredArgsConstructor
public class ScimUserHistoryController {

    private final UserQueryService userQueryService;

    /**
     * 특정 리비전 시점의 사용자 SCIM 프로필 조회
     * 
     * @param id    IAM User ID
     * @param revId SyncHistory에서 조회한 userRevId
     */
    @GetMapping("/{id}/revisions/{revId}")
    public ScimUserResponse getUserAtRevision(@PathVariable Long id, @PathVariable Long revId) {
        return userQueryService.getUserAtRevision(id, revId);
    }

    /**
     * 특정 시점의 사용자 SCIM 프로필 조회 (Transformed Data)
     * 
     * @param id      IAM User ID
     * @param traceId 장부 trace ID
     */
    @GetMapping("/{id}/trace_id/{traceId}")
    public ScimUserResponse getUserAtRevision(@PathVariable Long id, @PathVariable String traceId) {
        return userQueryService.getUserAtTraceId(id, traceId);
    }

    /**
     * 사용자의 리비전 이력 목록 조회 (페이징 및 필터링)
     * 
     * @param userId  IAM User ID (필터)
     * @param traceId 트레이스 ID (필터)
     */
    @GetMapping
    public Page<UserRevisionResponse> getUserHistories(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String traceId,
            @PageableDefault(size = 20) Pageable pageable) {
        return userQueryService.getUserRevisions(userId, traceId, pageable);
    }
}
