package com.iam.registry.domain.scim;

import com.iam.registry.domain.common.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "iam_attribute_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Builder
public class IamAttributeMeta {

    @Id
    @Column(name = "attribute_code", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_domain", nullable = false, length = 20)
    private AttributeTargetDomain targetDomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private AttributeCategory category;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private AttributeDataType type;

    @Builder.Default
    @Column(name = "multi_valued")
    private boolean multiValued = false;

    @Column(name = "scim_schema_uri")
    private String scimSchemaUri;

    @Column(name = "parent_name", length = 50)
    private String parentName;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "is_required")
    private boolean required = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "mutability", length = 20)
    private AttributeMutability mutability = AttributeMutability.READ_WRITE;

    @Builder.Default
    @Column(name = "admin_only")
    private boolean adminOnly = false;

    // Permissions (0=Public, 9=Admin)
    @Builder.Default
    @Column(name = "view_level")
    private int viewLevel = 0;

    @Builder.Default
    @Column(name = "edit_level")
    private int editLevel = 5;

    @Builder.Default
    @Column(name = "is_encrypted")
    private boolean encrypted = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "returned", length = 20)
    private AttributeReturned returned = AttributeReturned.DEFAULT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "uniqueness", length = 20)
    private AttributeUniqueness uniqueness = AttributeUniqueness.NONE;

    @Column(name = "ui_component")
    private String uiComponent;

    // For Code type, we might need a reference to code group.
    // Simplifying for now as per plan.
}
