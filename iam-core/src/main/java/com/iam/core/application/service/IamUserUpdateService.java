package com.iam.core.application.service;

import com.iam.core.domain.entity.*;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Service to apply transformed attributes to IamUser and its extensions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IamUserUpdateService {

    private final IamUserRepository iamUserRepository;

    private static final Set<String> CORE_ATTRIBUTES = Set.of(
            "userName", "active", "familyName", "givenName", "formattedName", "title");

    private static final String ENTERPRISE_URN = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

    /**
     * Updates or creates an IamUser based on transformed attributes.
     */
    @Transactional
    public IamUser update(IamUser user, Map<String, UniversalData> attributes) {
        log.debug("Updating IamUser: id={}, attributes={}", user.getId(), attributes.keySet());

        // 1. Update Core Fields
        applyCoreAttributes(user, attributes);

        // 2. Update Extensions
        updateExtensions(user, attributes);

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

    private void applyCoreAttributes(IamUser user, Map<String, UniversalData> attributes) {
        attributes.forEach((key, data) -> {
            if (CORE_ATTRIBUTES.contains(key) && !(data instanceof NullData)) {
                switch (key) {
                    case "userName" -> user.setUserName(data.asString());
                    case "active" -> {
                        if (data.getValue() instanceof Boolean b)
                            user.setActive(b);
                        else
                            user.setActive(Boolean.parseBoolean(data.asString()));
                    }
                    case "familyName" -> user.setFamilyName(data.asString());
                    case "givenName" -> user.setGivenName(data.asString());
                    case "formattedName" -> user.setFormattedName(data.asString());
                    case "title" -> user.setTitle(data.asString());
                }
            }
        });
    }

    private void updateExtensions(IamUser user, Map<String, UniversalData> attributes) {
        IamUserExtension ext = user.getExtension();
        if (ext == null) {
            ext = new IamUserExtension();
            ext.setUser(user);
            user.setExtension(ext);
        }

        // For simplicity in Phase 2/4, we assume all non-core attributes
        // that are not explicitly prefixed go to a GenericExtension or
        // EnterpriseExtension
        // In a more mature implementation, rules would return a nested map per URN.

        // Let's check if we have Enterprise attributes
        EnterpriseUserExtension enterprise = (EnterpriseUserExtension) ext.getExtensions().get(ENTERPRISE_URN);
        if (enterprise == null) {
            enterprise = new EnterpriseUserExtension();
        }

        boolean hasEnterprise = false;
        GenericExtension generic = (GenericExtension) ext.getExtensions().get("GenericExtension");
        if (generic == null)
            generic = new GenericExtension();

        for (Map.Entry<String, UniversalData> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if (CORE_ATTRIBUTES.contains(key))
                continue;

            UniversalData data = entry.getValue();

            // Heuristic for Enterprise fields (should match SCIM)
            switch (key) {
                case "employeeNumber" -> {
                    enterprise.setEmployeeNumber(data.asString());
                    hasEnterprise = true;
                }
                case "department" -> {
                    enterprise.setDepartment(data.asString());
                    hasEnterprise = true;
                }
                case "costCenter" -> {
                    enterprise.setCostCenter(data.asString());
                    hasEnterprise = true;
                }
                case "organization" -> {
                    enterprise.setOrganization(data.asString());
                    hasEnterprise = true;
                }
                case "division" -> {
                    enterprise.setDivision(data.asString());
                    hasEnterprise = true;
                }
                default -> generic.add(key, data.getValue());
            }
        }

        if (hasEnterprise) {
            ext.getExtensions().put(ENTERPRISE_URN, enterprise);
            if (!ext.getSchemas().contains(ENTERPRISE_URN))
                ext.getSchemas().add(ENTERPRISE_URN);
        }

        if (!generic.getAttributes().isEmpty()) {
            ext.getExtensions().put("GenericExtension", generic);
        }
    }
}
