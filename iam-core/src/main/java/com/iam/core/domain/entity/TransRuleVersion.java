package com.iam.core.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "IAM_TRANS_RULE_VERSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransRuleVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VER_ID")
    private Long verId;

    @Column(name = "RULE_ID", length = 50, nullable = false)
    private String ruleId;

    @Column(name = "VERSION_NO", nullable = false)
    private Integer versionNo;

    @Column(name = "SCRIPT_CONTENT", columnDefinition = "TEXT", nullable = false)
    private String scriptContent;

    @Column(name = "SCRIPT_HASH", length = 64, nullable = false)
    private String scriptHash;

    @Column(name = "CHANGE_LOG", columnDefinition = "TEXT")
    private String changeLog;

    @Builder.Default
    @Column(name = "IS_CURRENT")
    private Boolean isCurrent = false;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;
}
