package com.iam.core.application.common;

import java.util.List;

public record ScimResourceTypeDto(
                String id,
                String name,
                String description,
                String endpoint,
                String schema,
                List<SchemaExtensionDto> schemaExtensions) {

        public record SchemaExtensionDto(
                        String schema,
                        boolean required,
                        String name,
                        String description) {
        }
}
