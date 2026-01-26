package com.iam.core.domain.scim;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class ScimResourceTypeExtension {

    @Column(name = "extension_schema_uri", nullable = false)
    private String schema;

    @Column(name = "required", nullable = false)
    private boolean required;
}
