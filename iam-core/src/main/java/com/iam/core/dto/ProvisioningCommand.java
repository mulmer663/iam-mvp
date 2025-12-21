package com.iam.core.dto;

import java.util.Map;

public record ProvisioningCommand(
        String traceId,
        String causeEventId,
        String command,
        ProvisioningPayload payload) {
    public record ProvisioningPayload(
            String targetSystemId,
            Map<String, Object> attributes) {
    }
}
