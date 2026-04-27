package com.iam.registry.application.common;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * SCIM User response shape per RFC 7644 §3.4. Top-level fields are the
 * core User schema; extension URN-keyed objects are flattened from
 * {@link #extensions()} via {@code @JsonAnyGetter} so the same response
 * structure works for any registered customer extension URN — no
 * Java-side per-extension fields.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ScimUserResponse(
                List<String> schemas,
                String id,
                String externalId,
                String userName,
                Name name,
                String title,
                List<MultiValue> emails,
                List<MultiValue> phoneNumbers,
                List<Address> addresses,
                Boolean active,
                Meta meta,
                @JsonAnyGetter Map<String, Map<String, Object>> extensions) {
        @Builder
        public record Name(
                        String familyName,
                        String givenName,
                        String formatted) {
        }

        @Builder
        public record MultiValue(
                        String value,
                        String type,
                        boolean primary,
                        String display,
                        @JsonProperty("$ref") String ref) {
        }

        @Builder
        public record Address(
                        String streetAddress,
                        String locality,
                        String region,
                        String postalCode,
                        String country,
                        String type,
                        boolean primary,
                        String formatted) {
        }

        @Builder
        public record Meta(
                        String resourceType,
                        String created,
                        String lastModified,
                        String location,
                        String version) {
        }
}
