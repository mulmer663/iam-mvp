package com.iam.core.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "IAM_TRANS_RULE_META")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransRuleMeta {
    @Id
    @Column(name = "RULE_ID", length = 50)
    private String ruleId;

    @Column(name = "RULE_NAME", length = 100, nullable = false)
    private String ruleName;

    @Column(name = "TARGET_ATTR", length = 50, nullable = false)
    private String targetAttribute;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "STATUS", length = 20)
    private String status = "DRAFT"; // DRAFT, ACTIVE, RETIRED

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
