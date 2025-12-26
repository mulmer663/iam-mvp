package com.iam.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 정의되지 않은 커스텀 확장을 위한 Generic 클래스
 */
@NoArgsConstructor
public class GenericExtension extends ExtensionData {
    private final Map<String, Object> attributes = new HashMap<>();

    @JsonAnySetter
    public void add(String key, Object value) {
        attributes.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
