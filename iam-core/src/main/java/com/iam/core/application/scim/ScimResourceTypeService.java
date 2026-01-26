package com.iam.core.application.scim;

import com.iam.core.adapter.web.ScimEndpointManager;
import com.iam.core.application.common.ScimResourceTypeDto;
import com.iam.core.application.common.ScimSchemaDto;
import com.iam.core.domain.common.constant.ScimEndpointConstants;
import com.iam.core.domain.common.exception.ErrorCode;
import com.iam.core.domain.common.exception.IamBusinessException;
import com.iam.core.domain.scim.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScimResourceTypeService {

    private final ScimResourceTypeMetaRepository repository;
    private final ScimEndpointManager endpointManager;
    private final ScimSchemaService schemaService;
    private final ScimSchemaMetaRepository schemaRepository;

    @Transactional(readOnly = true)
    public List<ScimResourceTypeDto> getAllResourceTypes() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScimResourceTypeDto getResourceType(String id) {
        return repository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM",
                        "ResourceType not found: " + id));
    }

    @Transactional
    public ScimResourceTypeDto createResourceType(ScimResourceTypeDto dto) {
        ScimResourceTypeMeta entity = ScimResourceTypeMeta.builder()
                .id(dto.id())
                .name(dto.name())
                .description(dto.description())
                .endpoint(dto.endpoint())
                .schema(dto.schema())
                .schemaExtensions(dto.schemaExtensions().stream()
                        .map(ext -> ScimResourceTypeExtension.builder()
                                .schema(ext.schema())
                                .required(ext.required())
                                .build())
                        .collect(Collectors.toCollection(java.util.LinkedHashSet::new)))
                .build();
        ScimResourceTypeMeta saved = repository.save(entity);
        Objects.requireNonNull(saved, "Saved ResourceTypeMeta must not be null");

        // Auto-register schemas for extensions
        dto.schemaExtensions().forEach(this::ensureSchemaRegistered);

        // Register endpoint if not core type
        if (!ScimEndpointConstants.isCoreType(saved.getId())) {
            endpointManager.register(saved.getId(), saved.getEndpoint());
        }

        return mapToDto(saved);
    }

    @Transactional
    public ScimResourceTypeDto updateResourceType(String id, ScimResourceTypeDto dto) {
        ScimResourceTypeMeta entity = repository.findById(id)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM",
                        "ResourceType not found: " + id));

        String oldEndpoint = entity.getEndpoint();
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setEndpoint(dto.endpoint());
        entity.setSchema(dto.schema());
        entity.setSchemaExtensions(dto.schemaExtensions().stream()
                .map(ext -> ScimResourceTypeExtension.builder()
                        .schema(ext.schema())
                        .required(ext.required())
                        .build())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new)));

        ScimResourceTypeMeta saved = repository.save(entity);

        // Auto-register schemas for extensions
        dto.schemaExtensions().forEach(this::ensureSchemaRegistered);

        // Update registration if endpoint changed and not core type
        if (!ScimEndpointConstants.isCoreType(id)) {
            if (!dto.endpoint().equals(oldEndpoint)) {
                endpointManager.unregister(oldEndpoint);
                endpointManager.register(id, dto.endpoint());
            }
        }

        return mapToDto(saved);
    }

    @Transactional
    public void deleteResourceType(String id) {
        ScimResourceTypeMeta entity = repository.findById(id)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM",
                        "ResourceType not found: " + id));

        repository.delete(entity);

        // Unregister endpoint if not core type
        if (!ScimEndpointConstants.isCoreType(id)) {
            endpointManager.unregister(entity.getEndpoint());
        }
    }

    public ScimResourceTypeDto mapToDto(ScimResourceTypeMeta entity) {
        return new ScimResourceTypeDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getEndpoint(),
                entity.getSchema(),
                entity.getSchemaExtensions().stream()
                        .map(ext -> {
                            ScimSchemaMeta schemaMeta = schemaRepository.findById(ext.getSchema()).orElse(null);
                            return new ScimResourceTypeDto.SchemaExtensionDto(
                                    ext.getSchema(),
                                    ext.isRequired(),
                                    schemaMeta != null ? schemaMeta.getName() : null,
                                    schemaMeta != null ? schemaMeta.getDescription() : null);
                        })
                        .toList());
    }

    private void ensureSchemaRegistered(ScimResourceTypeDto.SchemaExtensionDto ext) {
        if (ext.name() != null && !ext.name().isBlank()) {
            if (!schemaRepository.existsById(ext.schema())) {
                schemaService.createSchema(new ScimSchemaDto(
                        ext.schema(),
                        ext.name(),
                        ext.description(),
                        List.of()));
            }
        }
    }
}
