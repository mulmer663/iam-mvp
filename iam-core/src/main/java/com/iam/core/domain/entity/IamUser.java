package com.iam.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "iam_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IamUser {
    @Id
    @Column(name = "user_id")
    private Long id; // TSID

    @Column(name = "external_id")
    private String externalId; // 원천 시스템 식별자

    @Column(nullable = false, unique = true)
    private String userName;

    // Flattened Name Attributes
    private String familyName;
    private String givenName;
    private String formattedName;

    private String title;

    private boolean active;

    // Meta Attributes (Flat)
    private String resourceType;
    private LocalDateTime created;
    private LocalDateTime lastModified;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private IamUserExtension extension;
}
