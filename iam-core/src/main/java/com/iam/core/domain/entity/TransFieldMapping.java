package com.iam.core.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "IAM_TRANS_FIELD_MAPPING")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransFieldMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MAPPING_ID")
    private Long id;

    @Column(name = "RULE_ID", length = 50, nullable = false)
    private String ruleId;

    @Column(name = "SOURCE_FIELD", length = 100, nullable = false)
    private String sourceField;

    @Column(name = "TARGET_FIELD", length = 100, nullable = false)
    private String targetField;

    @Builder.Default
    @Column(name = "IS_REQUIRED")
    private Boolean isRequired = false;

    @Column(name = "MIN_LENGTH")
    private Integer minLength;

    @Column(name = "MAX_LENGTH")
    private Integer maxLength;

    @Builder.Default
    @Column(name = "TRANSFORM_TYPE", length = 20, nullable = false)
    private String transformType = "DIRECT"; // DIRECT, CODE, CLASSIFY, REPLACE, CUSTOM

    @Column(name = "TRANSFORM_PARAMS", columnDefinition = "TEXT")
    private String transformParams;

    @Column(name = "CODE_GROUP_ID", length = 50)
    private String codeGroupId;

    @Column(name = "DEFAULT_VALUE", length = 255)
    private String defaultValue;

    @Column(name = "TRANSFORM_SCRIPT", columnDefinition = "TEXT")
    private String transformScript;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
