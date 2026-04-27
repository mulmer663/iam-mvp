package com.iam.registry.application.user;

import com.iam.registry.application.scim.EntityAttributeBinder;
import com.iam.registry.domain.common.ExtensionData;
import com.iam.registry.domain.common.enums.AttributeTargetDomain;
import com.iam.registry.domain.common.vo.UniversalData;
import com.iam.registry.domain.scim.IamAttributeMeta;
import com.iam.registry.domain.scim.IamAttributeMetaId;
import com.iam.registry.domain.scim.IamAttributeMetaRepository;
import com.iam.registry.domain.user.IamUser;
import com.iam.registry.domain.user.IamUserExtension;
import com.iam.registry.domain.user.IamUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

/**
 * Applies transformed attributes to an {@link IamUser} and its extension
 * map. CORE attributes are reflected onto the user entity; EXTENSION
 * attributes go into the schema-keyed {@code IamUserExtension.extensions}
 * map (URN -> ExtensionData) — no typed extension classes after the
 * generalization in commit 0baa75c.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IamUserUpdateService {

    private final IamUserRepository iamUserRepository;
    private final IamAttributeMetaRepository attributeMetaRepository;
    private final EntityAttributeBinder binder;

    @Transactional
    public IamUser update(IamUser user, Map<String, UniversalData> attributes) {
        log.debug("Updating IamUser: id={}, attributes={}", user.getId(), attributes.keySet());

        attributes.forEach((key, data) -> {
            // Composite PK lookup; same attribute name may legally exist under
            // multiple domains, but for a User update we always use USER.
            IamAttributeMeta meta = attributeMetaRepository
                    .findById(new IamAttributeMetaId(key, AttributeTargetDomain.USER))
                    .orElse(null);

            if (meta == null) {
                applyToGeneric(user, key, data);
                return;
            }

            switch (meta.getCategory()) {
                case CORE -> binder.bindCoreAttribute(user, meta, data);
                case EXTENSION -> applyToExtension(user, meta, data);
                case CUSTOM -> applyToGeneric(user, key, data);
            }
        });

        user.setLastModified(LocalDateTime.now());
        return iamUserRepository.save(user);
    }

    @Transactional
    public IamUser create(String externalId, Map<String, UniversalData> attributes) {
        log.info("Creating new IamUser for externalId: {}", externalId);

        IamUser user = new IamUser();
        user.setExternalId(externalId);
        user.setResourceType("User");
        user.setCreated(LocalDateTime.now());

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
        ExtensionData extensionData = ext.getExtensions().computeIfAbsent(schemaUri, k -> new ExtensionData());

        binder.bindExtensionAttribute(extensionData, meta, data);
        ext.getExtensions().put(schemaUri, extensionData);
        if (!ext.getSchemas().contains(schemaUri)) {
            ext.getSchemas().add(schemaUri);
        }
    }

    private void applyToGeneric(IamUser user, String key, UniversalData data) {
        IamUserExtension ext = getOrInitExtension(user);
        ExtensionData generic = ext.getExtensions().computeIfAbsent("__generic__", k -> new ExtensionData());
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
}
