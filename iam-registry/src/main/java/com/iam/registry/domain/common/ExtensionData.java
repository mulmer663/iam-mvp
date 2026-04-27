package com.iam.registry.domain.common;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Single concrete container for SCIM extension attribute values.
 *
 * <p>The shape of an extension is defined by metadata rows in
 * {@code IamAttributeMeta} (category=EXTENSION, scim_schema_uri=&lt;URN&gt;) —
 * <em>not</em> by Java subclasses. New customer extensions are added by
 * inserting metadata rows; no recompile required. The {@code IamUserExtension}
 * map is keyed by URN and each value carries the attribute key/value pairs
 * defined for that URN's schema.
 *
 * <p>Jackson's {@link JsonAnySetter} / {@link JsonAnyGetter} pair lets us
 * serialize arbitrary attribute names without a Java mirror class.
 */
@NoArgsConstructor
public class ExtensionData {
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
