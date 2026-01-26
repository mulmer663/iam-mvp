package com.iam.core.application.common;

import java.util.List;

public record ScimResourceTypeDto(
        String id,
        String name,
        String description,
        String endpoint,
        String schema,
        List<String> schemaExtensions) {
}
