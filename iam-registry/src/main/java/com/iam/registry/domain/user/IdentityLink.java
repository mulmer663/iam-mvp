package com.iam.registry.domain.user;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "iam_identity_link", indexes = {
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
    private Long iamUserId; // FK (?�리???�결)

    @Column(name = "system_type", nullable = false)
    private String systemType; // "HR", "AD", "SAP"

    @Column(name = "external_id", nullable = false)
    private String externalId; // ?? ?�번 "2023001", AD "hong.g"

    // ??계정???�재 ?�태 (??��?�어???�력 ?��?�??�해 Row???��?)
    @Column(name = "is_active")
    private boolean active;
}
