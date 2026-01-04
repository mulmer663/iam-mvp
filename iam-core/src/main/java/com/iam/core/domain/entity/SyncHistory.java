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
    private java.util.Map<String, Object> resultData; // 변경 내역(Diff) 또는 에러 상세 정보

    @Column(name = "user_rev_id", nullable = false)
    private Long userRevId; // 실제 사용자의 변경 리비전

    @Column(name = "rule_rev_id", nullable = false)
    private Long ruleRevId; // 변환 규칙의 리비전

    @Builder.Default
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "parent_history_id")
    private Long parentHistoryId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
