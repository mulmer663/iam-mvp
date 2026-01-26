package com.iam.core.application.scim;

import com.iam.core.application.common.IamAttributeMetaDto;
import com.iam.core.domain.common.enums.AttributeCategory;
import com.iam.core.domain.common.enums.AttributeTargetDomain;
import com.iam.core.domain.common.exception.ErrorCode;
import com.iam.core.domain.common.exception.IamBusinessException;
import com.iam.core.domain.scim.IamAttributeMeta;
import com.iam.core.domain.scim.IamAttributeMetaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IamAttributeMetaService {
    private final IamAttributeMetaRepository repository;

    @Transactional
    public IamAttributeMetaDto createAttribute(IamAttributeMetaDto dto) {
        if (repository.existsById(dto.name())) {
            throw new IamBusinessException(ErrorCode.VALIDATION_FAILED, "INTERNAL",
                    "Attribute name already exists: " + dto.name());
        }

        IamAttributeMeta entity = IamAttributeMeta.builder()
                .name(dto.name())
                .targetDomain(dto.targetDomain())
                .category(dto.category())
                .displayName(dto.displayName())
                .type(dto.type())
                .multiValued(dto.multiValued())
                .scimSchemaUri(dto.scimSchemaUri())
                .parentName(dto.parentName())
                .description(dto.description())
                .required(dto.required())
                .mutability(dto.mutability())
                .returned(dto.returned())
                .uniqueness(dto.uniqueness())
                .adminOnly(dto.adminOnly())
                .viewLevel(dto.viewLevel())
                .editLevel(dto.editLevel())
                .encrypted(dto.encrypted())
                .uiComponent(dto.uiComponent())
                .build();

        IamAttributeMeta saved = repository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public IamAttributeMetaDto updateAttribute(String name, IamAttributeMetaDto updates) {
        IamAttributeMeta attribute = repository.findById(name)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "INTERNAL",
                        "Attribute not found: " + name));

        // Immutable checks
        if (!attribute.getCategory().equals(updates.category())) {
            throw new IamBusinessException(ErrorCode.VALIDATION_FAILED, "INTERNAL",
                    "Cannot change Attribute Category");
        }
        if (!attribute.getTargetDomain().equals(updates.targetDomain())) {
            throw new IamBusinessException(ErrorCode.VALIDATION_FAILED, "INTERNAL",
                    "Cannot change Target Domain");
        }
        if (!attribute.getType().equals(updates.type())) {
            throw new IamBusinessException(ErrorCode.VALIDATION_FAILED, "INTERNAL",
                    "Cannot change Data Type");
        }

        // Apply updates
        attribute.setDisplayName(updates.displayName());
        attribute.setDescription(updates.description());
        attribute.setRequired(updates.required());
        attribute.setMutability(updates.mutability());
        attribute.setMultiValued(updates.multiValued());
        attribute.setReturned(updates.returned());
        attribute.setUniqueness(updates.uniqueness());
        attribute.setAdminOnly(updates.adminOnly());
        attribute.setViewLevel(updates.viewLevel());
        attribute.setEditLevel(updates.editLevel());
        attribute.setEncrypted(updates.encrypted());
        attribute.setUiComponent(updates.uiComponent());
        // SCIM Schema URI logic? Maybe allow update if EXTENSION?
        if (attribute.getCategory() == AttributeCategory.EXTENSION) {
            attribute.setScimSchemaUri(updates.scimSchemaUri());
        }

        return mapToDto(attribute);
    }

    @Transactional
    public void deleteAttribute(String name) {
        IamAttributeMeta attribute = repository.findById(name)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "INTERNAL",
                        "Attribute not found: " + name));

        if (attribute.getCategory() == AttributeCategory.CORE) {
            throw new IamBusinessException(ErrorCode.VALIDATION_FAILED, "INTERNAL",
                    "Cannot delete CORE attributes");
        }

        repository.delete(attribute);
    }

    public List<IamAttributeMetaDto> getAttributes(AttributeTargetDomain domain) {
        List<IamAttributeMeta> attributes;
        if (domain != null) {
            attributes = repository.findByTargetDomain(domain);
        } else {
            attributes = repository.findAll();
        }
        return attributes.stream().map(this::mapToDto).toList();
    }

    public List<IamAttributeMetaDto> getExtensionAttributes(AttributeTargetDomain domain) {
        return repository.findByTargetDomainAndCategory(domain, AttributeCategory.EXTENSION)
                .stream().map(this::mapToDto).toList();
    }

    private IamAttributeMetaDto mapToDto(IamAttributeMeta entity) {
        return new IamAttributeMetaDto(
                entity.getName(),
                entity.getTargetDomain(),
                entity.getCategory(),
                entity.getDisplayName(),
                entity.getType(),
                entity.getScimSchemaUri(),
                entity.getParentName(),
                entity.getDescription(),
                entity.isRequired(),
                entity.getMutability(),
                entity.isMultiValued(),
                entity.getReturned(),
                entity.getUniqueness(),
                entity.isAdminOnly(),
                entity.getViewLevel(),
                entity.getEditLevel(),
                entity.isEncrypted(),
                entity.getUiComponent());
    }
}
