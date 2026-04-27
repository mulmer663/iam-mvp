package com.iam.registry.application.user;

import com.iam.registry.application.common.ScimListResponse;
import com.iam.registry.application.common.ScimUserResponse;
import com.iam.registry.domain.common.ExtensionData;
import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.IamBusinessException;
import com.iam.registry.domain.user.IamUser;
import com.iam.registry.domain.user.IamUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-side service for User. Builds {@link ScimUserResponse} from
 * {@link IamUser}. Extension URNs other than the core User schema are
 * surfaced via {@link ScimUserResponse#extensions()} so any registered
 * customer extension is rendered without per-URN code branches.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private static final String USER_CORE_URN = "urn:ietf:params:scim:schemas:core:2.0:User";

    private final IamUserRepository iamUserRepository;

    public ScimListResponse<ScimUserResponse> getAllUsers() {
        List<ScimUserResponse> scimUsers = iamUserRepository.findAll().stream()
                .map(this::toScimUser)
                .toList();
        return new ScimListResponse<>(scimUsers);
    }

    public ScimUserResponse getUserById(Long id) {
        return iamUserRepository.findById(id)
                .map(this::toScimUser)
                .orElseThrow(() -> new IamBusinessException(
                        ErrorCode.USER_NOT_FOUND, "API", "User not found: " + id));
    }

    private ScimUserResponse toScimUser(IamUser user) {
        // Build the schemas list: always the core User schema, plus any
        // extension URNs that have non-empty data on this user.
        List<String> schemas = new ArrayList<>();
        schemas.add(USER_CORE_URN);

        Map<String, Map<String, Object>> extensions = new LinkedHashMap<>();
        if (user.getExtension() != null && user.getExtension().getExtensions() != null) {
            for (Map.Entry<String, ExtensionData> entry : user.getExtension().getExtensions().entrySet()) {
                String urn = entry.getKey();
                if ("__generic__".equals(urn)) continue;
                Map<String, Object> data = entry.getValue() == null ? Map.of() : new HashMap<>(entry.getValue().getAttributes());
                if (!data.isEmpty()) {
                    extensions.put(urn, data);
                    if (!schemas.contains(urn)) schemas.add(urn);
                }
            }
        }

        List<ScimUserResponse.MultiValue> emails = user.getEmails().stream()
                .map(e -> ScimUserResponse.MultiValue.builder()
                        .value(e.getValue()).type(e.getType()).primary(e.isPrimary()).display(e.getDisplay())
                        .build())
                .toList();

        List<ScimUserResponse.MultiValue> phoneNumbers = user.getPhoneNumbers().stream()
                .map(p -> ScimUserResponse.MultiValue.builder()
                        .value(p.getValue()).type(p.getType()).primary(p.isPrimary()).display(p.getDisplay())
                        .build())
                .toList();

        List<ScimUserResponse.Address> addresses = user.getAddresses().stream()
                .map(a -> ScimUserResponse.Address.builder()
                        .streetAddress(a.getStreetAddress())
                        .locality(a.getLocality())
                        .region(a.getRegion())
                        .postalCode(a.getPostalCode())
                        .country(a.getCountry())
                        .type(a.getType())
                        .primary(a.isPrimary())
                        .formatted(a.getFormatted())
                        .build())
                .toList();

        return ScimUserResponse.builder()
                .schemas(schemas)
                .id(String.valueOf(user.getId()))
                .externalId(user.getExternalId())
                .userName(user.getUserName())
                .name(new ScimUserResponse.Name(
                        user.getFamilyName(),
                        user.getGivenName(),
                        user.getFormattedName()))
                .title(user.getTitle())
                .emails(emails.isEmpty() ? null : emails)
                .phoneNumbers(phoneNumbers.isEmpty() ? null : phoneNumbers)
                .addresses(addresses.isEmpty() ? null : addresses)
                .active(user.isActive())
                .extensions(extensions.isEmpty() ? null : extensions)
                .meta(ScimUserResponse.Meta.builder()
                        .resourceType("User")
                        .created(user.getCreated() != null ? user.getCreated().format(DateTimeFormatter.ISO_DATE_TIME) : null)
                        .lastModified(user.getLastModified() != null ? user.getLastModified().format(DateTimeFormatter.ISO_DATE_TIME) : null)
                        .location("/scim/v2/Users/" + user.getId())
                        .version(user.getVersion() != null ? "W/\"" + user.getVersion() + "\"" : null)
                        .build())
                .build();
    }
}
