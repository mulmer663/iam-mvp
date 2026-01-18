package com.iam.core.application.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ScimResourceTypeResponse(
        String id,
        String name,
        String description,
        String endpoint,
        String schema,
        List<SchemaExtension> schemaExtensions) {
    @Builder
    public record SchemaExtension(
            String schema,
            boolean required) {
    }
}
