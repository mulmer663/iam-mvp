package com.iam.registry.domain.scim;

import com.iam.registry.domain.common.enums.AttributeTargetDomain;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite key for {@link IamAttributeMeta}: (name, targetDomain).
 *
 * The same SCIM attribute name may exist under different target domains
 * (e.g., User.displayName and Group.displayName per RFC 7643), so the
 * primary key must include the domain.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IamAttributeMetaId implements Serializable {
    private String name;
    private AttributeTargetDomain targetDomain;
}
