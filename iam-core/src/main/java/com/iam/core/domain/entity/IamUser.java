package com.iam.core.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "iam_user")
@Getter
@Setter
@NoArgsConstructor
public class IamUser {
    @Id
    @Column(name = "user_id", length = 36)
    private String id; // UUID

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId; // IAM 로그인용 ID

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private UserStatus status; // ACTIVE, INACTIVE

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private IamUserExtension extension;

    // CreatedAt, UpdatedAt (Audit) could be added here, but following spec strictly
    // for now.
}
