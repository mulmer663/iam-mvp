package com.iam.core.dto;

import java.util.Map;

public record UserSyncPayload(
        String hrEmpId,
        String name,
        Map<String, Object> attributes) {
}
