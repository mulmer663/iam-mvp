package com.iam.core.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScimMultiValue {
    private String value;
    private String type; // e.g., "work", "home", "mobile"
    @Column(name = "is_primary")
    private boolean primary;
    private String display;
    private String ref; // For groups/manager ($ref)
}
