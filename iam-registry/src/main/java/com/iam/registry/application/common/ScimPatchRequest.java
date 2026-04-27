package com.iam.registry.application.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * RFC 7644 §3.5.2 PATCH request envelope.
 * "Operations" is capital-O per the spec wire format.
 */
public record ScimPatchRequest(
        @JsonProperty("schemas") List<String> schemas,
        @JsonProperty("Operations") List<PatchOperation> operations
) {
    public record PatchOperation(
            String op,
            String path,
            Object value
    ) {}
}
