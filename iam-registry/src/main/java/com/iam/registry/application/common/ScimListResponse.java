package com.iam.registry.application.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * RFC 7644 §3.4.2 — ListResponse 응답.
 * totalResults = 필터 기준 전체 개수 (현재 페이지 아님).
 * count=0 요청 시 resources는 빈 리스트, totalResults는 전체 개수.
 */
public record ScimListResponse<T>(
        List<String> schemas,
        int totalResults,
        int itemsPerPage,
        int startIndex,
        @JsonProperty("Resources") List<T> resources) {

    private static final List<String> LIST_RESPONSE_SCHEMA =
            List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse");

    /** 페이징 없는 전체 조회 (하위 호환) */
    public ScimListResponse(List<T> resources) {
        this(
                LIST_RESPONSE_SCHEMA,
                resources.size(),
                resources.size(),
                1,
                resources);
    }

    /** 페이징 포함 응답 */
    public static <T> ScimListResponse<T> paged(
            List<T> resources, int totalResults, int startIndex) {
        return new ScimListResponse<>(
                LIST_RESPONSE_SCHEMA,
                totalResults,
                resources.size(),
                startIndex,
                resources);
    }

    /** count=0 요청: 리소스 없이 totalResults만 반환 */
    public static <T> ScimListResponse<T> countOnly(int totalResults, int startIndex) {
        return new ScimListResponse<>(
                LIST_RESPONSE_SCHEMA,
                totalResults,
                0,
                startIndex,
                List.of());
    }
}
