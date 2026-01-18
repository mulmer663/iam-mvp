package com.iam.core.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScimAddress {
    private String streetAddress;
    private String locality;
    private String region;
    private String postalCode;
    private String country;
    private String type; // e.g., "work", "home"
    private boolean primary;
    private String formatted;
}
