package com.iam.core.application.common;

import lombok.Builder;

import java.util.List;

@Builder
public record ScimSchemaResponse(
        String id,
        String name,
        String description,
        List<ScimAttribute> attributes) {
    @Builder
    public record ScimAttribute(
            String name,
            String type,
            boolean multiValued,
            String description,
            boolean required,
            String mutability,
            String returned,
            String uniqueness,
            List<ScimAttribute> subAttributes) {
    }
}
