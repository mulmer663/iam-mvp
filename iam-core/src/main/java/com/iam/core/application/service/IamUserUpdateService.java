package com.iam.core.application.service;

import com.iam.core.domain.entity.*;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.vo.UniversalData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to apply transformed attributes to IamUser and its extensions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IamUserUpdateService {

    private final IamUserRepository iamUserRepository;
    private final com.iam.core.domain.repository.IamAttributeMetaRepository attributeMetaRepository;
    private final EntityAttributeBinder binder;

    private static final String ENTERPRISE_URN = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

    /**
     * Updates or creates an IamUser based on transformed attributes.
     */
    @Transactional
    public IamUser update(IamUser user, Map<String, UniversalData> attributes) {
        log.debug("Updating IamUser: id={}, attributes={}", user.getId(), attributes.keySet());

        // Load all relevant metadata
        Map<String, IamAttributeMeta> metaMap = attributeMetaRepository.findAllById(attributes.keySet())
                .stream()
                .collect(Collectors.toMap(IamAttributeMeta::getName, m -> m));

        // Process Attributes
        attributes.forEach((key, data) -> {
            IamAttributeMeta meta = metaMap.get(key);

            if (meta == null) {
                applyToGeneric(user, key, data);
            } else {
                switch (meta.getCategory()) {
                    case CORE -> binder.bindCoreAttribute(user, meta, data);
                    case EXTENSION -> applyToExtension(user, meta, data);
                    case CUSTOM -> applyToGeneric(user, key, data);
                }
            }
        });

        user.setLastModified(LocalDateTime.now());
        return iamUserRepository.save(user);
    }

    /**
     * Creates a new IamUser and applies attributes.
     */
    @Transactional
    public IamUser create(String externalId, Map<String, UniversalData> attributes) {
        log.info("Creating new IamUser for externalId: {}", externalId);

        IamUser user = new IamUser();
        user.setExternalId(externalId);
        user.setResourceType("User");
        user.setCreated(LocalDateTime.now());

        // Initialize extension
        IamUserExtension ext = new IamUserExtension();
        ext.setUser(user);
        ext.setSchemas(new ArrayList<>());
        user.setExtension(ext);

        return update(user, attributes);
    }

    private void applyToExtension(IamUser user, IamAttributeMeta meta, UniversalData data) {
        String schemaUri = meta.getScimSchemaUri();
        if (schemaUri == null) {
            applyToGeneric(user, meta.getName(), data);
            return;
        }

        IamUserExtension ext = getOrInitExtension(user);
        ExtensionData extensionData = ext.getExtensions().get(schemaUri);

        if (extensionData == null) {
            extensionData = createExtension(schemaUri);
        }

        binder.bindExtensionAttribute(extensionData, meta, data);
        ext.getExtensions().put(schemaUri, extensionData);
        if (!ext.getSchemas().contains(schemaUri)) {
            ext.getSchemas().add(schemaUri);
        }
    }

    private void applyToGeneric(IamUser user, String key, UniversalData data) {
        IamUserExtension ext = getOrInitExtension(user);
        GenericExtension generic = (GenericExtension) ext.getExtensions()
                .computeIfAbsent("GenericExtension", k -> new GenericExtension());
        generic.add(key, data.getValue());
    }

    private IamUserExtension getOrInitExtension(IamUser user) {
        IamUserExtension ext = user.getExtension();
        if (ext == null) {
            ext = new IamUserExtension();
            ext.setUser(user);
            ext.setSchemas(new ArrayList<>());
            user.setExtension(ext);
        }
        return ext;
    }

    private ExtensionData createExtension(String schemaUri) {
        if (ENTERPRISE_URN.equals(schemaUri)) {
            return new EnterpriseUserExtension();
        }
        return new GenericExtension();
    }
}
