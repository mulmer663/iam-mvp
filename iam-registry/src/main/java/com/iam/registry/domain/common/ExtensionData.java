package com.iam.registry.domain.common;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.iam.registry.domain.user.EnterpriseUserExtension;
import lombok.NoArgsConstructor;

/**
 * SCIM Extension??베이???�래??
 * ?�려�??�키�?Enterprise ?????�속???�해 ?�???�전???�드�??�공?�니??
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "urn", defaultImpl = GenericExtension.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EnterpriseUserExtension.class, name = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"),
        @JsonSubTypes.Type(value = GenericExtension.class, name = "GenericExtension")
})
@NoArgsConstructor
public abstract class ExtensionData {
    private final java.util.Map<String, Object> attributes = new java.util.HashMap<>();

    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void add(String key, Object value) {
        attributes.put(key, value);
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    public java.util.Map<String, Object> getAttributes() {
        return attributes;
    }
}
