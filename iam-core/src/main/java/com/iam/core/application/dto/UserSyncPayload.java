package com.iam.core.application.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Builder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSyncPayload {

        @NotBlank(message = "externalId는 필수입니다")
        private String externalId;

        @NotBlank(message = "userName은 필수입니다")
        @Size(min = 2, max = 100, message = "userName은 2자 이상 100자 이하여야 합니다")
        private String userName;

        @Valid
        @Builder.Default
        private Name name = new Name();

        @Size(max = 200, message = "title은 200자 이하여야 합니다")
        private String title;

        @NotNull(message = "active 상태는 필수입니다")
        private Boolean active;

        @Builder.Default
        private Map<String, Object> extensions = new HashMap<>();

        @JsonAnySetter
        public void addExtension(String key, Object value) {
                if (key == null)
                        return;

                // Manually route known SCIM name fields if they come in flat
                // This is a common requirement for flat-to-nested mapping from dynamic sources
                switch (key) {
                        case "familyName", "lastName" -> {
                                if (value != null)
                                        name.setFamilyName(value.toString());
                        }
                        case "givenName", "firstName" -> {
                                if (value != null)
                                        name.setGivenName(value.toString());
                        }
                        case "formattedName", "formatted", "displayName" -> {
                                if (value != null)
                                        name.setFormatted(value.toString());
                        }
                        default -> this.extensions.put(key, value);
                }
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Name {
                @com.fasterxml.jackson.annotation.JsonAlias({ "familyName", "lastName" })
                private String familyName;

                @com.fasterxml.jackson.annotation.JsonAlias({ "givenName", "firstName" })
                private String givenName;

                @com.fasterxml.jackson.annotation.JsonAlias({ "formatted", "formattedName", "displayName" })
                private String formatted;
        }
}