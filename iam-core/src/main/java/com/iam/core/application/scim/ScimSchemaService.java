package com.iam.core.application.scim;

import com.iam.core.application.common.ScimSchemaDto;
import com.iam.core.domain.common.exception.ErrorCode;
import com.iam.core.domain.common.exception.IamBusinessException;
import com.iam.core.domain.scim.ScimSchemaMeta;
import com.iam.core.domain.scim.ScimSchemaMetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScimSchemaService {

    private final ScimSchemaMetaRepository repository;

    @Transactional(readOnly = true)
    public List<ScimSchemaDto> getAllSchemas() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScimSchemaDto getSchema(String uri) {
        return repository.findById(uri)
                .map(this::mapToDto)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM",
                        "Schema not found: " + uri));
    }

    @Transactional
    public ScimSchemaDto createSchema(ScimSchemaDto dto) {
        if (repository.existsById(dto.id())) {
            throw new IamBusinessException(ErrorCode.VALIDATION_FAILED, "SCIM",
                    "Schema ID already exists: " + dto.id());
        }
        ScimSchemaMeta entity = ScimSchemaMeta.builder()
                .id(dto.id())
                .name(dto.name())
                .description(dto.description())
                .build();
        ScimSchemaMeta saved = repository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public ScimSchemaDto updateSchema(String uri, ScimSchemaDto dto) {
        ScimSchemaMeta entity = repository.findById(uri)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM",
                        "Schema not found: " + uri));

        entity.setName(dto.name());
        entity.setDescription(dto.description());

        ScimSchemaMeta saved = repository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteSchema(String uri) {
        if (!repository.existsById(uri)) {
            throw new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM", "Schema not found: " + uri);
        }
        repository.deleteById(uri);
    }

    private ScimSchemaDto mapToDto(ScimSchemaMeta entity) {
        return new ScimSchemaDto(entity.getId(), entity.getName(), entity.getDescription());
    }
}
