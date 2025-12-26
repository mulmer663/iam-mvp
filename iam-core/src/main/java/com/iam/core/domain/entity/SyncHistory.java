package com.iam.core.domain.entity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SyncHistory {
    @Id
    @Tsid
    @Column(name = "history_id")
    private Long id; // TSID

    private String traceId;

    @Column(nullable = false)
    private String type; // HR_SYNC, USER_UPDATE, AD_PROVISION

    private String status; // SUCCESS, FAILURE

    private String targetUser; // UserName or ID

    @Column(columnDefinition = "TEXT")
    private String payload; // JSON payload

    private String message; // Error message or details

    private LocalDateTime createdAt;
}
