package com.iam.core.domain.user;

import com.iam.core.domain.common.vo.ScimAddress;
import com.iam.core.domain.common.vo.ScimMultiValue;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "iam_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class IamUser {
    @Id
    @Column(name = "user_id")
    @Tsid
    private Long id; // TSID

    @Version
    private Long version; // For SCIM meta.version (ETag)

    @Column(name = "external_id")
    private String externalId; // 원천 시스템 식별자

    @Column(nullable = false, unique = true)
    private String userName;

    // Flattened Name Attributes
    private String familyName;
    private String givenName;
    private String formattedName;

    private String displayName;
    private String nickName;
    private String profileUrl;

    private String title;
    private String userType;
    private String preferredLanguage;
    private String locale;
    private String timezone;

    private boolean active;

    // Multi-valued Attributes
    @ElementCollection
    @CollectionTable(name = "iam_user_emails", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<ScimMultiValue> emails = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "iam_user_phone_numbers", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<ScimMultiValue> phoneNumbers = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "iam_user_addresses", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<ScimAddress> addresses = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "iam_user_ims", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<ScimMultiValue> ims = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "iam_user_photos", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<ScimMultiValue> photos = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "iam_user_groups", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<ScimMultiValue> groups = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "iam_user_entitlements", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<ScimMultiValue> entitlements = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "iam_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<ScimMultiValue> roles = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "iam_user_x509_certificates", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<ScimMultiValue> x509Certificates = new HashSet<>();

    // Meta Attributes (Flat)
    private String resourceType;
    private LocalDateTime created;
    private LocalDateTime lastModified;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private IamUserExtension extension;
}
