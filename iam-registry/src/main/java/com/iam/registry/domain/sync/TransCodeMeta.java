package com.iam.registry.domain.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "IAM_TRANS_CODE_META")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransCodeMeta {
    @Id
    @Column(name = "CODE_GROUP_ID", length = 50)
    private String codeGroupId;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
}
