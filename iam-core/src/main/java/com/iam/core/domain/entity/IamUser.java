package com.iam.core.domain.entity;

import com.iam.core.domain.vo.ScimAddress;
import com.iam.core.domain.vo.ScimMultiValue;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "iam_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    private String title;

    private boolean active;

    // Multi-valued Attributes
    @ElementCollection
    @CollectionTable(name = "iam_user_emails", joinColumns = @JoinColumn(name = "user_id"))
    private List<ScimMultiValue> emails = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "iam_user_phone_numbers", joinColumns = @JoinColumn(name = "user_id"))
    private List<ScimMultiValue> phoneNumbers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "iam_user_addresses", joinColumns = @JoinColumn(name = "user_id"))
    private List<ScimAddress> addresses = new ArrayList<>();

    // Meta Attributes (Flat)
    private String resourceType;
    private LocalDateTime created;
    private LocalDateTime lastModified;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private IamUserExtension extension;
}
