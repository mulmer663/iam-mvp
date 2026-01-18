package com.iam.core.application.service;

import com.iam.core.application.dto.ScimUserResponse;
import com.iam.core.domain.entity.IamUser;
import com.iam.core.domain.exception.ErrorCode;
import com.iam.core.domain.exception.IamBusinessException;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private void applyAdvancedAttributes(IamUser user, Map<String, Object> scimUser) {
        // 1. Emails
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

        // 2. Phone Numbers
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

        // 3. Addresses
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

    private String asString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private boolean asBoolean(Object obj) {
        return obj != null && Boolean.parseBoolean(obj.toString());
    }

    @Transactional
    public void deleteUser(Long id) {
        iamUserRepository.deleteById(id);
    }

    private Map<String, UniversalData> convertToUniversalData(Map<String, Object> scimUser) {
        Map<String, UniversalData> attributes = new HashMap<>();

        // 1. Core Simple Attributes
        if (scimUser.get("userName") != null) {
            attributes.put("userName", new StringData(scimUser.get("userName").toString()));
        }
        if (scimUser.get("active") != null) {
            attributes.put("active", new BooleanData(asBoolean(scimUser.get("active"))));
        }
        if (scimUser.get("title") != null) {
            attributes.put("title", new StringData(scimUser.get("title").toString()));
        }

        // 2. Core Complex Attributes (Name)
        Object nameObj = scimUser.get("name");
        if (nameObj instanceof Map<?, ?> nameMap) {
            if (nameMap.get("familyName") != null) {
                attributes.put("familyName", new StringData(nameMap.get("familyName").toString()));
            }
            if (nameMap.get("givenName") != null) {
                attributes.put("givenName", new StringData(nameMap.get("givenName").toString()));
            }
            if (nameMap.get("formatted") != null) {
                attributes.put("formattedName", new StringData(nameMap.get("formatted").toString()));
            }
        }

        // 3. Enterprise Extension
        String enterpriseUrn = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";
        Object entObj = scimUser.get(enterpriseUrn);
        if (entObj instanceof Map<?, ?> entMap) {
            entMap.forEach((k, v) -> {
                if (v != null) {
                    attributes.put(k.toString(), new StringData(v.toString()));
                }
            });
        }

        return attributes;
    }
}
