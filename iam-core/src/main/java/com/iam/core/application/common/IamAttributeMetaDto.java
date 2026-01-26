package com.iam.core.application.common;

import com.iam.core.domain.common.enums.*;

public record IamAttributeMetaDto(
                String name,
                AttributeTargetDomain targetDomain,
                AttributeCategory category,
                String displayName,
                AttributeDataType type,
                String scimSchemaUri,
                String parentName,
                String description,
                boolean required,
                AttributeMutability mutability,
                boolean multiValued,
                AttributeReturned returned,
                AttributeUniqueness uniqueness,
                boolean adminOnly,
                int viewLevel,
                int editLevel,
                boolean encrypted,
                String uiComponent) {
}
