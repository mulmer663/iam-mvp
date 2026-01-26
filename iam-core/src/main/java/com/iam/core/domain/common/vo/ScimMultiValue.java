package com.iam.core.domain.common.vo;

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
public class ScimMultiValue {
    @Column(name = "attr_value")
    private String value;
    @Column(name = "attr_type")
    private String type; // e.g., "work", "home", "mobile"
    @Column(name = "is_primary")
    private boolean primary;
    @Column(name = "attr_display")
    private String display;
    @Column(name = "attr_ref")
    private String ref; // For groups/manager ($ref)
}
