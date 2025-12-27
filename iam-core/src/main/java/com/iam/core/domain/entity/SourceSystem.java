package com.iam.core.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "IAM_SOURCE_SYSTEM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceSystem {
    @Id
    @Column(name = "SYSTEM_ID", length = 50)
    private String systemId;

    @Column(name = "SYSTEM_NAME", length = 100, nullable = false)
    private String systemName;

    @Column(name = "SYSTEM_TYPE", length = 20, nullable = false)
    private String systemType; // AD, JDBC, REST, CSV, etc.

    @Column(name = "CONN_INFO", columnDefinition = "jsonb", nullable = false)
    private String connectionInfo;

    @Builder.Default
    @Column(name = "IS_ACTIVE")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
