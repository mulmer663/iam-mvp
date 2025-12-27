package com.iam.core.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ScimUserResponse(
                List<String> schemas,
                String id,
                String externalId,
                String userName,
                Name name,
                String title,
                List<Email> emails,
                boolean active,
                Meta meta,
                @JsonProperty("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User") Map<String, Object> enterpriseExtension,
                @JsonProperty("urn:ietf:params:scim:schemas:extension:custom:2.0:User") Map<String, Object> customExtension) {
        @Builder
        public record Name(
                        String familyName,
                        String givenName,
                        String formatted) {
        }

        @Builder
        public record Email(
                        String value,
                        boolean primary) {
        }

        @Builder
        public record Meta(
                        String resourceType,
                        String created,
                        String lastModified,
                        String location) {
        }
}
