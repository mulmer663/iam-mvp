package com.iam.core.domain.scim;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scim_resource_type_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class ScimResourceTypeMeta {

    @Id
    @Column(name = "resource_type_id", nullable = false, length = 50)
    private String id; // e.g., "User"

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "endpoint", nullable = false)
    private String endpoint; // e.g., "/Users"

    @Column(name = "schema_uri", nullable = false)
    private String schema; // Main Schema URN

    @ElementCollection
    @CollectionTable(name = "scim_resource_type_extensions", joinColumns = @JoinColumn(name = "resource_type_id"))
    @Column(name = "extension_schema_uri")
    @Builder.Default
    private List<String> schemaExtensions = new ArrayList<>();
}
