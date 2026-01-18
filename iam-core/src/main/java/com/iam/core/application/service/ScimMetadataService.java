package com.iam.core.application.service;

import com.iam.core.application.dto.ScimResourceTypeResponse;
import com.iam.core.application.dto.ScimSchemaResponse;
import com.iam.core.domain.entity.ScimResourceTypeMeta;
import com.iam.core.domain.entity.ScimSchemaMeta;
import com.iam.core.domain.exception.ErrorCode;
import com.iam.core.domain.exception.IamBusinessException;
import com.iam.core.domain.repository.ScimResourceTypeMetaRepository;
import com.iam.core.domain.repository.ScimSchemaMetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScimMetadataService {

    private final ScimSchemaMetaRepository scimSchemaMetaRepository;
    private final ScimResourceTypeMetaRepository scimResourceTypeMetaRepository;

    public List<ScimSchemaResponse> getAllSchemas() {
        return scimSchemaMetaRepository.findAll().stream()
                .map(this::toSchemaResponse)
                .toList();
    }

    public ScimSchemaResponse getSchema(String uri) {
        return scimSchemaMetaRepository.findById(uri)
                .map(this::toSchemaResponse)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM",
                        "Schema not found: " + uri));
    }

    public List<ScimResourceTypeResponse> getAllResourceTypes() {
        return scimResourceTypeMetaRepository.findAll().stream()
                .map(this::toResourceTypeResponse)
                .toList();
    }

    public ScimResourceTypeResponse getResourceType(String id) {
        return scimResourceTypeMetaRepository.findById(id)
                .map(this::toResourceTypeResponse)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM",
                        "ResourceType not found: " + id));
    }

    private ScimSchemaResponse toSchemaResponse(ScimSchemaMeta meta) {
        return ScimSchemaResponse.builder()
                .id(meta.getId())
                .name(meta.getName())
                .description(meta.getDescription())
                .attributes(meta.getAttributes().stream()
                        .map(attr -> ScimSchemaResponse.ScimAttribute.builder()
                                .name(attr.getName())
                                .type(attr.getType().name().toLowerCase())
                                .multiValued(attr.isMultiValued())
                                .description(attr.getDescription())
                                .required(attr.isRequired())
                                .mutability(attr.getMutability().name().toLowerCase())
                                .returned(attr.getReturned().name().toLowerCase())
                                .uniqueness(attr.getUniqueness().name().toLowerCase())
                                .build())
                        .toList())
                .build();
    }

    private ScimResourceTypeResponse toResourceTypeResponse(ScimResourceTypeMeta meta) {
        return ScimResourceTypeResponse.builder()
                .id(meta.getId())
                .name(meta.getName())
                .description(meta.getDescription())
                .endpoint(meta.getEndpoint())
                .schema(meta.getSchema())
                .schemaExtensions(meta.getSchemaExtensions().stream()
                        .map(ext -> ScimResourceTypeResponse.SchemaExtension.builder()
                                .schema(ext)
                                .required(false)
                                .build())
                        .toList())
                .build();
    }
}
