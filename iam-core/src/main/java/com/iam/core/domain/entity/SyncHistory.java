package com.iam.core.domain.entity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_history", indexes = {
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

    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // Human readable message

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // Copy of requestPayload/responsePayload for CSV compat

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload; // Raw snapshot or pre-transformation data

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload; // Final SCIM/IAM data or error details

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
