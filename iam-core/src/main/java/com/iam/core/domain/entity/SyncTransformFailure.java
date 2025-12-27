package com.iam.core.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "IAM_SYNC_TRANSFORM_FAILURE", indexes = {
        @Index(name = "idx_transform_history", columnList = "history_id"),
        @Index(name = "idx_transform_field", columnList = "field_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncTransformFailure {
    @Id
    @Column(name = "failure_id")
    private Long failureId; // TSID

    @Column(name = "history_id", nullable = false)
    private Long historyId;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "invalid_value", columnDefinition = "TEXT")
    private String invalidValue;

    @Column(name = "rule_name", length = 100)
    private String ruleName;

    @Column(name = "error_type", length = 50)
    private String errorType; // MISSING, INVALID_FORMAT, DUPLICATE

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "suggested_fix", columnDefinition = "TEXT")
    private String suggestedFix;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
