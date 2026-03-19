package com.iam.registry.domain.sync;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "IAM_TRANS_CODE_VALUE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransCodeValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VAL_ID")
    private Long id;

    @Column(name = "CODE_GROUP_ID", length = 50, nullable = false)
    private String codeGroupId;

    @Column(name = "SOURCE_VALUE", length = 100, nullable = false)
    private String sourceValue;

    @Column(name = "TARGET_VALUE", length = 100, nullable = false)
    private String targetValue;

    @Column(name = "LABEL", length = 100)
    private String label; // e.g., "?�원", "?��?
}
