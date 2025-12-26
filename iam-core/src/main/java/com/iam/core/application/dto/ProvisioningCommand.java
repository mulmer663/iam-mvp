package com.iam.core.application.dto;

import java.util.Map;

public record ProvisioningCommand(
                String traceId,
                String causeEventId,
                String command,
                ProvisioningPayload payload) {

        public record ProvisioningPayload(
                        String targetSystemId, // userName 혹은 식별자
                        String familyName,
                        String givenName,
                        String formattedName,
                        boolean active,
                        Map<String, Object> attributes) { // Extensions
        }
}
