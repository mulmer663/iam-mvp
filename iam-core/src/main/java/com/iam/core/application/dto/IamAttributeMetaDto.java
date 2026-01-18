package com.iam.core.application.dto;

import com.iam.core.domain.enums.AttributeCategory;
import com.iam.core.domain.enums.AttributeDataType;
import com.iam.core.domain.enums.AttributeMutability;
import com.iam.core.domain.enums.AttributeTargetDomain;

public record IamAttributeMetaDto(
        String code,
        AttributeTargetDomain targetDomain,
        AttributeCategory category,
        String displayName,
        AttributeDataType dataType,
        String scimSchemaUri,
        String description,
        boolean required,
        AttributeMutability mutability,
        boolean adminOnly,
        int viewLevel,
        int editLevel,
        boolean encrypted,
        String uiComponent) {
}
