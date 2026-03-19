package com.iam.registry.domain.sync;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "IAM_TRANS_MAPPING")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MAP_ID")
    private Long mapId;

    @Column(name = "SYSTEM_ID", length = 50, nullable = false)
    private String systemId;

    @Column(name = "RULE_ID", length = 50, nullable = false)
    private String ruleId;

    @Builder.Default
    @Column(name = "EXEC_ORDER")
    private Integer execOrder = 0;

    @Builder.Default
    @Column(name = "IS_MANDATORY")
    private Boolean isMandatory = true;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
}
