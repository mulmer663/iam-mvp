package com.iam.core.application.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
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
public class UserSyncPayload {

        @NotBlank(message = "externalId는 필수입니다")
        private String externalId;

        @NotBlank(message = "userName은 필수입니다")
        @Size(min = 2, max = 100, message = "userName은 2자 이상 100자 이하여야 합니다")
        private String userName;

        @Valid
        private Name name;

        @Size(max = 200, message = "title은 200자 이하여야 합니다")
        private String title;

        @NotNull(message = "active 상태는 필수입니다")
        private Boolean active;

        private Map<String, Object> extensions = new HashMap<>();

        @JsonAnySetter
        public void addExtension(String key, Object value) {
                this.extensions.put(key, value);
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Name {
                @Size(max = 50, message = "familyName은 50자 이하여야 합니다")
                private String familyName;

                @Size(max = 50, message = "givenName은 50자 이하여야 합니다")
                private String givenName;

                @Size(max = 100, message = "formatted는 100자 이하여야 합니다")
                private String formatted;
        }
}