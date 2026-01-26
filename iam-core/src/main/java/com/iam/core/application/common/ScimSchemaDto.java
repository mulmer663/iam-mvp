package com.iam.core.application.common;

import java.util.List;

public record ScimSchemaDto(
                String id, // URN
                String name,
                String description,
                List<AttributeDto> attributes) {

        public record AttributeDto(
                        String name,
                        String type,
                        boolean multiValued,
                        String description,
                        boolean required,
                        String mutability,
                        String returned,
                        String uniqueness,
                        List<AttributeDto> subAttributes) {
        }
}
