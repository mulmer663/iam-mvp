package com.iam.core.domain.scim;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "scim_dynamic_resource", indexes = {
        @Index(name = "idx_scim_res_type", columnList = "resource_type"),
        @Index(name = "idx_scim_res_scim_id", columnList = "scim_id", unique = true),
        @Index(name = "idx_scim_res_ext_id", columnList = "external_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ScimDynamicResource {

    @Id
    @Tsid
    @Column(name = "id")
    private Long id;

    @Column(name = "scim_id", nullable = false, length = 64, unique = true)
    private String scimId; // URL-friendly unique ID (UUID or custom string like 'my-device-01')

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType; // e.g., "Device", "Location"

    @Column(name = "external_id", length = 100)
    private String externalId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Version
    @Column(name = "version")
    private Long version;
}
