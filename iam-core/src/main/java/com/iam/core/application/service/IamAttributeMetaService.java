package com.iam.core.application.service;

import com.iam.core.application.dto.IamAttributeMetaDto;
import com.iam.core.domain.entity.IamAttributeMeta;
import com.iam.core.domain.enums.AttributeCategory;
import com.iam.core.domain.enums.AttributeTargetDomain;
import com.iam.core.domain.repository.IamAttributeMetaRepository;
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
    public IamAttributeMetaDto createAttribute(IamAttributeMeta attribute) {
        if (repository.existsById(attribute.getCode())) {
            throw new IllegalArgumentException("Attribute code already exists: " + attribute.getCode());
        }
        // Force valid category/domain rules if needed?
        // For now trusting input but could add more validation.
        IamAttributeMeta saved = repository.save(attribute);
        return mapToDto(saved);
    }

    @Transactional
    public IamAttributeMetaDto updateAttribute(String code, IamAttributeMetaDto updates) {
        IamAttributeMeta attribute = repository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Attribute not found: " + code));

        // Immutable checks
        if (!attribute.getCategory().equals(updates.category())) {
            throw new IllegalArgumentException("Cannot change Attribute Category");
        }
        if (!attribute.getTargetDomain().equals(updates.targetDomain())) {
            throw new IllegalArgumentException("Cannot change Target Domain");
        }
        if (!attribute.getDataType().equals(updates.dataType())) {
            throw new IllegalArgumentException("Cannot change Data Type");
        }

        // Apply updates
        attribute.setDisplayName(updates.displayName());
        attribute.setDescription(updates.description());
        attribute.setRequired(updates.required());
        attribute.setMutability(updates.mutability());
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
    public void deleteAttribute(String code) {
        IamAttributeMeta attribute = repository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Attribute not found: " + code));

        if (attribute.getCategory() == AttributeCategory.CORE) {
            throw new IllegalArgumentException("Cannot delete CORE attributes");
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
                entity.getCode(),
                entity.getTargetDomain(),
                entity.getCategory(),
                entity.getDisplayName(),
                entity.getDataType(),
                entity.getScimSchemaUri(),
                entity.getDescription(),
                entity.isRequired(),
                entity.getMutability(),
                entity.isAdminOnly(),
                entity.getViewLevel(),
                entity.getEditLevel(),
                entity.isEncrypted(),
                entity.getUiComponent());
    }
}
