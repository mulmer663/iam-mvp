package com.iam.core.application.dto;

import com.iam.core.domain.enums.*;

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
