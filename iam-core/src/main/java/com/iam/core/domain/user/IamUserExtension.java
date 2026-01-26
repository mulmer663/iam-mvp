package com.iam.core.domain.user;

import com.iam.core.domain.common.ExtensionData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "iam_user_extension")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class IamUserExtension {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private IamUser user;

    // Standard Relational Mapping for schemas
    @ElementCollection
    @CollectionTable(name = "iam_user_schema", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "schema_uri")
    private List<String> schemas = new ArrayList<>();

    // Truly dynamic extensions remain JSONB to avoid schema drift
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, ExtensionData> extensions = new HashMap<>();
    // Key: urn:ietf:params:scim:schemas:extension:...
}
