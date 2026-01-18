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
@EqualsAndHashCode
public class ScimAddress {
    private String streetAddress;
    private String locality;
    private String region;
    private String postalCode;
    private String country;
    @Column(name = "attr_type")
    private String type; // e.g., "work", "home"
    @Column(name = "is_primary")
    private boolean primary;
    private String formatted;
}
