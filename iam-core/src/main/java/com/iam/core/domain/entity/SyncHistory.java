package com.iam.core.domain.entity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import java.time.LocalDateTime;

@Entity
@Table(name = "iam_sync_history", indexes = {
        @Index(name = "idx_sync_trace", columnList = "trace_id"),
        @Index(name = "idx_sync_target", columnList = "target_user")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncHistory {
    @Id
    @Tsid
    @Column(name = "history_id")
    private Long id; // TSID

    @Column(name = "trace_id", length = 64, nullable = false)
    private String traceId;

    @Column(length = 32, nullable = false)
    private String type; // HR_SYNC, USER_UPDATE, AD_PROVISION

    @Column(length = 20)
    private String status; // SUCCESS, FAILURE, PARTIAL_SUCCESS

    @Column(name = "source_system", length = 100)
    private String sourceSystem; // e.g., "SAP_HR", "WORKDAY"

    @Column(name = "target_system", length = 100)
    private String targetSystem; // e.g., "AZURE_AD", "LOCAL_LDAP"

    @Column(name = "target_user")
    private String targetUser;

    @Column(name = "iam_user_id")
    private Long iamUserId;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // Human readable message

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb")
    private java.util.Map<String, Object> requestPayload; // Raw snapshot or pre-transformation data

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "result_data", columnDefinition = "jsonb")
    private java.util.Map<String, Object> resultData; // Final SCIM/IAM data or error details

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "applied_rules", columnDefinition = "jsonb")
    private java.util.List<Long> appliedRules; // List of TransRuleVersion IDs

    @Builder.Default
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "parent_history_id")
    private Long parentHistoryId;

    @Column(name = "duration_ms")
    private Long durationMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
