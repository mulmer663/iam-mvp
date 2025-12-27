package com.iam.core.domain.entity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "identity_link", indexes = {
        @Index(name = "idx_link_external", columnList = "system_type, external_id")
})
@Getter
@Setter
@NoArgsConstructor
public class IdentityLink {
    @Id
    @Tsid
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iam_user_id", nullable = false)
    private Long iamUserId; // FK (논리적 연결)

    @Column(name = "system_type", nullable = false)
    private String systemType; // "HR", "AD", "SAP"

    @Column(name = "external_id", nullable = false)
    private String externalId; // 예: 사번 "2023001", AD "hong.g"

    // 이 계정의 현재 상태 (삭제되어도 이력 유지를 위해 Row는 남김)
    @Column(name = "is_active")
    private boolean active;
}
