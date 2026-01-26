package com.iam.core.application.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ScimListResponse<T>(
        List<String> schemas,
        int totalResults,
        int itemsPerPage,
        int startIndex,
        @JsonProperty("Resources") List<T> resources) {
    public ScimListResponse(List<T> resources) {
        this(
                List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
                resources.size(),
                resources.size(),
                1,
                resources);
    }
}
