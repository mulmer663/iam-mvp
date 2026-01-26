package com.iam.core.application.scim;

import com.iam.core.application.common.ScimResourceTypeDto;
import com.iam.core.domain.common.exception.ErrorCode;
import com.iam.core.domain.common.exception.IamBusinessException;
import com.iam.core.domain.scim.ScimResourceTypeMeta;
import com.iam.core.domain.scim.ScimResourceTypeMetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScimResourceTypeService {

    private final ScimResourceTypeMetaRepository repository;

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
        if (repository.existsById(dto.id())) {
            throw new IamBusinessException(ErrorCode.VALIDATION_FAILED, "SCIM",
                    "ResourceType ID already exists: " + dto.id());
        }
        ScimResourceTypeMeta entity = ScimResourceTypeMeta.builder()
                .id(dto.id())
                .name(dto.name())
                .description(dto.description())
                .endpoint(dto.endpoint())
                .schema(dto.schema())
                .schemaExtensions(dto.schemaExtensions())
                .build();
        ScimResourceTypeMeta saved = repository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public ScimResourceTypeDto updateResourceType(String id, ScimResourceTypeDto dto) {
        ScimResourceTypeMeta entity = repository.findById(id)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM",
                        "ResourceType not found: " + id));

        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setEndpoint(dto.endpoint());
        entity.setSchema(dto.schema());
        entity.setSchemaExtensions(dto.schemaExtensions());

        ScimResourceTypeMeta saved = repository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteResourceType(String id) {
        if (!repository.existsById(id)) {
            throw new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM", "ResourceType not found: " + id);
        }
        repository.deleteById(id);
    }

    private ScimResourceTypeDto mapToDto(ScimResourceTypeMeta entity) {
        return new ScimResourceTypeDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getEndpoint(),
                entity.getSchema(),
                List.copyOf(entity.getSchemaExtensions()));
    }
}
