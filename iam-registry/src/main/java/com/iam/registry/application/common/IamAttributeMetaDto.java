package com.iam.registry.application.common;

import com.iam.registry.domain.common.enums.*;

import java.util.List;

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
                boolean caseExact,
                List<String> canonicalValues,
                List<String> referenceTypes,
                boolean adminOnly,
                int viewLevel,
                int editLevel,
                boolean encrypted,
                String uiComponent,
                boolean display) {
}
