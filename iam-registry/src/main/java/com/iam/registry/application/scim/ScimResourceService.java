package com.iam.registry.application.scim;

import com.iam.registry.application.common.ScimUserResponse;
import com.iam.registry.application.user.IamUserUpdateService;
import com.iam.registry.application.user.UserQueryService;
import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.IamBusinessException;
import com.iam.registry.domain.common.vo.BooleanData;
import com.iam.registry.domain.common.vo.ScimAddress;
import com.iam.registry.domain.common.vo.ScimMultiValue;
import com.iam.registry.domain.common.vo.StringData;
import com.iam.registry.domain.common.vo.UniversalData;
import com.iam.registry.domain.user.IamUser;
import com.iam.registry.domain.user.IamUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SCIM 2.0 User resource façade. Translates inbound SCIM JSON into the
 * {@link UniversalData} attribute map consumed by
 * {@link IamUserUpdateService}, then re-reads via {@link UserQueryService}
 * so the response carries server-assigned fields (id, meta, version).
 */
@Service
@RequiredArgsConstructor
public class ScimResourceService {

    private final IamUserUpdateService iamUserUpdateService;
    private final IamUserRepository iamUserRepository;
    private final UserQueryService userQueryService;

    @Transactional
    public ScimUserResponse createUser(Map<String, Object> scimUser) {
        String externalId = (String) scimUser.get("externalId");
        Map<String, UniversalData> attributes = convertToUniversalData(scimUser);

        IamUser user = iamUserUpdateService.create(externalId, attributes);
        applyAdvancedAttributes(user, scimUser);

        return userQueryService.getUserById(user.getId());
    }

    @Transactional
    public ScimUserResponse updateUser(Long id, Map<String, Object> scimUser) {
        IamUser user = iamUserRepository.findById(id)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.USER_NOT_FOUND, "SCIM", "User not found: " + id));

        Map<String, UniversalData> attributes = convertToUniversalData(scimUser);
        iamUserUpdateService.update(user, attributes);
        applyAdvancedAttributes(user, scimUser);

        return userQueryService.getUserById(id);
    }

    @Transactional
    public void deleteUser(Long id) {
        iamUserRepository.deleteById(id);
    }

    /** Multi-valued (emails / phoneNumbers / addresses) — written through reflection */
    private void applyAdvancedAttributes(IamUser user, Map<String, Object> scimUser) {
        if (scimUser.get("emails") instanceof List<?> list) {
            user.getEmails().clear();
            list.forEach(item -> {
                if (item instanceof Map<?, ?> map) {
                    user.getEmails().add(ScimMultiValue.builder()
                            .value(asString(map.get("value")))
                            .type(asString(map.get("type")))
                            .primary(asBoolean(map.get("primary")))
                            .display(asString(map.get("display")))
                            .build());
                }
            });
        }

        if (scimUser.get("phoneNumbers") instanceof List<?> list) {
            user.getPhoneNumbers().clear();
            list.forEach(item -> {
                if (item instanceof Map<?, ?> map) {
                    user.getPhoneNumbers().add(ScimMultiValue.builder()
                            .value(asString(map.get("value")))
                            .type(asString(map.get("type")))
                            .primary(asBoolean(map.get("primary")))
                            .display(asString(map.get("display")))
                            .build());
                }
            });
        }

        if (scimUser.get("addresses") instanceof List<?> list) {
            user.getAddresses().clear();
            list.forEach(item -> {
                if (item instanceof Map<?, ?> map) {
                    user.getAddresses().add(ScimAddress.builder()
                            .streetAddress(asString(map.get("streetAddress")))
                            .locality(asString(map.get("locality")))
                            .region(asString(map.get("region")))
                            .postalCode(asString(map.get("postalCode")))
                            .country(asString(map.get("country")))
                            .type(asString(map.get("type")))
                            .primary(asBoolean(map.get("primary")))
                            .formatted(asString(map.get("formatted")))
                            .build());
                }
            });
        }

        iamUserRepository.save(user);
    }

    private Map<String, UniversalData> convertToUniversalData(Map<String, Object> scimUser) {
        Map<String, UniversalData> attributes = new HashMap<>();

        // 1. Core scalar attributes
        if (scimUser.get("userName") != null) attributes.put("userName", new StringData(scimUser.get("userName").toString()));
        if (scimUser.get("active") != null) attributes.put("active", new BooleanData(asBoolean(scimUser.get("active"))));
        if (scimUser.get("title") != null) attributes.put("title", new StringData(scimUser.get("title").toString()));
        if (scimUser.get("displayName") != null) attributes.put("displayName", new StringData(scimUser.get("displayName").toString()));
        if (scimUser.get("nickName") != null) attributes.put("nickName", new StringData(scimUser.get("nickName").toString()));

        // 2. Core complex (name)
        if (scimUser.get("name") instanceof Map<?, ?> nameMap) {
            if (nameMap.get("familyName") != null) attributes.put("familyName", new StringData(nameMap.get("familyName").toString()));
            if (nameMap.get("givenName") != null) attributes.put("givenName", new StringData(nameMap.get("givenName").toString()));
            if (nameMap.get("formatted") != null) attributes.put("formattedName", new StringData(nameMap.get("formatted").toString()));
        }

        // 3. Any extension URN — flatten its inner map into individual UniversalData entries.
        // Extension attributes are looked up by leaf name; IamUserUpdateService finds them via
        // composite-PK lookup on (name, USER) and routes them to the right schema URN.
        for (Map.Entry<String, Object> entry : scimUser.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("urn:")) continue;
            if (entry.getValue() instanceof Map<?, ?> extMap) {
                extMap.forEach((k, v) -> {
                    if (v != null) {
                        attributes.put(k.toString(), new StringData(v.toString()));
                    }
                });
            }
        }

        return attributes;
    }

    private String asString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private boolean asBoolean(Object obj) {
        return obj != null && Boolean.parseBoolean(obj.toString());
    }
}
