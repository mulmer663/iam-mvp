package com.iam.registry.application.scim;

import com.iam.registry.application.common.ScimPatchRequest;
import com.iam.registry.application.common.ScimPatchRequest.PatchOperation;
import com.iam.registry.application.common.ScimUserResponse;
import com.iam.registry.application.user.UserQueryService;
import com.iam.registry.domain.common.ExtensionData;
import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.IamBusinessException;
import com.iam.registry.domain.common.vo.ScimAddress;
import com.iam.registry.domain.common.vo.ScimMultiValue;
import com.iam.registry.domain.user.IamUser;
import com.iam.registry.domain.user.IamUserExtension;
import com.iam.registry.domain.user.IamUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Applies RFC 7644 §3.5.2 PATCH Operations to an IamUser.
 *
 * Supported path formats:
 *   - simple core attr          : "userName", "active", "title"
 *   - name sub-attribute        : "name.familyName", "name.givenName"
 *   - multi-valued              : "emails", "phoneNumbers", "addresses"
 *   - extension URN-qualified   : "urn:…:User:department"
 *   - no path (bulk map)        : op="replace", value={"userName":"x",…}
 *
 * Filter expressions (e.g. emails[type eq "work"].value) are NOT supported;
 * replace / add on the collection as a whole is recommended instead.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScimPatchService {

    private static final String OP_ADD = "add";
    private static final String OP_REMOVE = "remove";
    private static final String OP_REPLACE = "replace";

    private final IamUserRepository iamUserRepository;
    private final UserQueryService userQueryService;

    @Transactional
    public ScimUserResponse patch(Long id, ScimPatchRequest request) {
        IamUser user = iamUserRepository.findById(id)
                .orElseThrow(() -> new IamBusinessException(
                        ErrorCode.USER_NOT_FOUND, "SCIM", "User not found: " + id));

        for (PatchOperation op : request.operations()) {
            log.debug("Applying PATCH op={} path={}", op.op(), op.path());
            applyOperation(user, op);
        }

        user.setLastModified(LocalDateTime.now());
        iamUserRepository.save(user);
        return userQueryService.getUserById(id);
    }

    private void applyOperation(IamUser user, PatchOperation op) {
        String path = op.path();
        Object value = op.value();
        String opName = op.op() == null ? OP_REPLACE : op.op().toLowerCase();

        // No path → value must be a map (partial resource representation)
        if (path == null || path.isBlank()) {
            if (value instanceof Map<?, ?> map) {
                map.forEach((k, v) -> applyScalar(user, k.toString(), opName, v));
            }
            return;
        }

        // Extension URN attribute: urn:…:schemaId:attrName
        if (path.startsWith("urn:")) {
            int lastColon = path.lastIndexOf(':');
            String urn = path.substring(0, lastColon);
            String attrName = path.substring(lastColon + 1);
            applyExtension(user, urn, attrName, opName, value);
            return;
        }

        // Multi-valued collections
        switch (path) {
            case "emails"       -> applyMultiValue(user.getEmails(), opName, value);
            case "phoneNumbers" -> applyMultiValue(user.getPhoneNumbers(), opName, value);
            case "addresses"    -> applyAddressCollection(user.getAddresses(), opName, value);
            default -> {
                // name sub-attribute: name.familyName
                if (path.startsWith("name.")) {
                    applyNameSub(user, path.substring(5), opName, value);
                } else {
                    applyScalar(user, path, opName, value);
                }
            }
        }
    }

    private void applyScalar(IamUser user, String attr, String op, Object value) {
        if (OP_REMOVE.equals(op)) value = null;
        switch (attr) {
            case "userName"      -> user.setUserName(asString(value));
            case "active"        -> user.setActive(asBoolean(value));
            case "title"         -> user.setTitle(asString(value));
            case "externalId"    -> user.setExternalId(asString(value));
            default -> log.warn("PATCH: unsupported scalar path '{}' — ignored", attr);
        }
    }

    private void applyNameSub(IamUser user, String sub, String op, Object value) {
        if (OP_REMOVE.equals(op)) value = null;
        switch (sub) {
            case "familyName"  -> user.setFamilyName(asString(value));
            case "givenName"   -> user.setGivenName(asString(value));
            case "formatted"   -> user.setFormattedName(asString(value));
            default -> log.warn("PATCH: unsupported name sub-attr '{}' — ignored", sub);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyMultiValue(Collection<ScimMultiValue> collection, String op, Object value) {
        switch (op) {
            case OP_REMOVE -> collection.clear();
            case OP_ADD -> {
                if (value instanceof List<?> list) {
                    list.forEach(item -> {
                        if (item instanceof Map<?, ?> m) collection.add(toMultiValue((Map<String, Object>) m));
                    });
                } else if (value instanceof Map<?, ?> m) {
                    collection.add(toMultiValue((Map<String, Object>) m));
                }
            }
            default -> {  // replace
                collection.clear();
                if (value instanceof List<?> list) {
                    list.forEach(item -> {
                        if (item instanceof Map<?, ?> m) collection.add(toMultiValue((Map<String, Object>) m));
                    });
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void applyAddressCollection(Collection<ScimAddress> collection, String op, Object value) {
        switch (op) {
            case OP_REMOVE -> collection.clear();
            case OP_ADD -> {
                if (value instanceof List<?> list) {
                    list.forEach(item -> {
                        if (item instanceof Map<?, ?> m) collection.add(toAddress((Map<String, Object>) m));
                    });
                }
            }
            default -> {  // replace
                collection.clear();
                if (value instanceof List<?> list) {
                    list.forEach(item -> {
                        if (item instanceof Map<?, ?> m) collection.add(toAddress((Map<String, Object>) m));
                    });
                }
            }
        }
    }

    private void applyExtension(IamUser user, String urn, String attrName, String op, Object value) {
        IamUserExtension ext = getOrInitExtension(user);
        ExtensionData data = ext.getExtensions().computeIfAbsent(urn, k -> new ExtensionData());

        if (OP_REMOVE.equals(op)) {
            data.getAttributes().remove(attrName);
        } else {
            data.add(attrName, value);
        }

        ext.getExtensions().put(urn, data);
        if (!ext.getSchemas().contains(urn)) ext.getSchemas().add(urn);
    }

    private IamUserExtension getOrInitExtension(IamUser user) {
        if (user.getExtension() == null) {
            IamUserExtension ext = new IamUserExtension();
            ext.setUser(user);
            ext.setSchemas(new ArrayList<>());
            user.setExtension(ext);
        }
        return user.getExtension();
    }

    @SuppressWarnings("unchecked")
    private ScimMultiValue toMultiValue(Map<String, Object> m) {
        return ScimMultiValue.builder()
                .value(asString(m.get("value")))
                .type(asString(m.get("type")))
                .primary(asBoolean(m.get("primary")))
                .display(asString(m.get("display")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private ScimAddress toAddress(Map<String, Object> m) {
        return ScimAddress.builder()
                .streetAddress(asString(m.get("streetAddress")))
                .locality(asString(m.get("locality")))
                .region(asString(m.get("region")))
                .postalCode(asString(m.get("postalCode")))
                .country(asString(m.get("country")))
                .type(asString(m.get("type")))
                .primary(asBoolean(m.get("primary")))
                .formatted(asString(m.get("formatted")))
                .build();
    }

    private String asString(Object o) { return o == null ? null : o.toString(); }
    private boolean asBoolean(Object o) { return o != null && Boolean.parseBoolean(o.toString()); }
}
