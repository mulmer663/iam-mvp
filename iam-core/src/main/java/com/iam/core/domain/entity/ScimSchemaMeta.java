package com.iam.core.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scim_schema_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class ScimSchemaMeta {

    @Id
    @Column(name = "schema_uri", nullable = false, length = 100)
    private String id; // URN (e.g., "urn:ietf:params:scim:schemas:core:2.0:User")

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "scim_schema_uri", insertable = false, updatable = false)
    @Builder.Default
    private List<IamAttributeMeta> attributes = new ArrayList<>();
}
