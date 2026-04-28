package com.iam.registry.application.scim;

import com.iam.registry.application.common.ScimSchemaDto;
import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.IamBusinessException;
import com.iam.registry.domain.scim.IamAttributeMeta;
import com.iam.registry.domain.scim.IamAttributeMetaRepository;
import com.iam.registry.domain.scim.ScimResourceTypeMeta;
import com.iam.registry.domain.scim.ScimResourceTypeMetaRepository;
import com.iam.registry.domain.scim.ScimSchemaMeta;
import com.iam.registry.domain.scim.ScimSchemaMetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScimSchemaService {

    private final ScimSchemaMetaRepository repository;
    private final IamAttributeMetaRepository attributeRepository;
    private final ScimResourceTypeMetaRepository resourceTypeRepository;

    private static final List<String> RFC_STANDARD_PREFIXES = List.of(
            "urn:ietf:params:scim:schemas:core:2.0:",
            "urn:ietf:params:scim:schemas:extension:enterprise:2.0:");

    private boolean isRfcStandardSchema(String uri) {
        return uri != null && RFC_STANDARD_PREFIXES.stream().anyMatch(uri::startsWith);
    }

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
        if (isRfcStandardSchema(uri)) {
            throw new IamBusinessException(ErrorCode.VALIDATION_FAILED, "SCIM",
                    "Cannot delete RFC standard schema: " + uri);
        }

        // 1. Detach this schema from any ResourceType.schemaExtensions
        List<ScimResourceTypeMeta> rts = resourceTypeRepository.findAll();
        for (ScimResourceTypeMeta rt : rts) {
            boolean removed = rt.getSchemaExtensions().removeIf(ext -> uri.equals(ext.getSchema()));
            if (removed) {
                resourceTypeRepository.save(rt);
                log.info("Detached schema {} from ResourceType {}", uri, rt.getId());
            }
        }

        // 2. Cascade-delete attributes belonging to this schema
        List<IamAttributeMeta> orphaned = attributeRepository.findByScimSchemaUri(uri);
        if (!orphaned.isEmpty()) {
            attributeRepository.deleteAllInBatch(orphaned);
            log.info("Cascade-deleted {} attributes for schema {}", orphaned.size(), uri);
        }

        // 3. Delete the schema itself
        repository.deleteById(uri);
        log.info("Deleted schema {}", uri);
    }

    public ScimSchemaDto mapToDto(ScimSchemaMeta entity) {
        List<IamAttributeMeta> allAttributes = entity.getAttributes();
        List<ScimSchemaDto.AttributeDto> rootAttributes = allAttributes.stream()
                .filter(attr -> attr.getParentName() == null)
                .map(attr -> buildAttributeDto(attr, allAttributes))
                .toList();

        return new ScimSchemaDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                rootAttributes);
    }

    private ScimSchemaDto.AttributeDto buildAttributeDto(IamAttributeMeta attr,
            List<IamAttributeMeta> allAttributes) {
        List<ScimSchemaDto.AttributeDto> subAttributes = allAttributes.stream()
                .filter(sub -> attr.getName().equals(sub.getParentName()))
                .map(sub -> buildAttributeDto(sub, allAttributes))
                .toList();

        // If the name is namespaced (e.g. 'emails.value'), use only the leaf segment.
        String scimName = attr.getName();
        if (scimName.contains(".")) {
            scimName = scimName.substring(scimName.lastIndexOf(".") + 1);
        }

        return new ScimSchemaDto.AttributeDto(
                scimName,
                attr.getType().name().toLowerCase(),
                attr.isMultiValued(),
                attr.getDescription(),
                attr.isRequired(),
                attr.getMutability().name().toLowerCase(),
                attr.getReturned().name().toLowerCase(),
                attr.getUniqueness().name().toLowerCase(),
                attr.isCaseExact(),
                attr.getCanonicalValues() == null ? List.of() : attr.getCanonicalValues(),
                attr.getReferenceTypes() == null ? List.of() : attr.getReferenceTypes(),
                subAttributes.isEmpty() ? null : subAttributes);
    }
}
