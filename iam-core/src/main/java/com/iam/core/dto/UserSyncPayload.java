package com.iam.core.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
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

        private String externalId;
        private String userName;
        private Name name;
        private String title;
        private boolean active;

        private Map<String, Object> extensions = new HashMap<>();

        @JsonAnySetter
        public void addExtension(String key, Object value) {
                if (key.startsWith("urn:ietf:params:scim:schemas:extension:")) {
                        if (extensions == null) {
                                extensions = new HashMap<>();
                        }
                        extensions.put(key, value);
                }
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Name {
                private String familyName;
                private String givenName;
                private String formatted;
        }
}
