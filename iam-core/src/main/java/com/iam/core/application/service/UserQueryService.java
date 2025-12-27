package com.iam.core.application.service;

import com.iam.core.application.dto.ScimListResponse;
import com.iam.core.application.dto.ScimUserResponse;
import com.iam.core.domain.entity.EnterpriseUserExtension;
import com.iam.core.domain.entity.ExtensionData;
import com.iam.core.domain.entity.IamUser;
import com.iam.core.domain.exception.ErrorCode;
import com.iam.core.domain.exception.IamBusinessException;
import com.iam.core.domain.repository.IamUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

        private final IamUserRepository iamUserRepository;
        private static final String ENTERPRISE_URN = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

        public ScimListResponse<ScimUserResponse> getAllUsers() {
                List<IamUser> users = iamUserRepository.findAll();
                List<ScimUserResponse> scimUsers = users.stream()
                                .map(this::toScimUser)
                                .toList();
                return new ScimListResponse<>(scimUsers);
        }

        public ScimUserResponse getUserById(Long id) {
                return iamUserRepository.findById(id)
                                .map(this::toScimUser)
                                .orElseThrow(() -> new IamBusinessException(
                                                ErrorCode.USER_NOT_FOUND,
                                                "API",
                                                "User not found: " + id));
        }

        private ScimUserResponse toScimUser(IamUser user) {
                Map<String, Object> enterpriseData = null;
                if (user.getExtension() != null && user.getExtension().getExtensions() != null) {
                        ExtensionData ext = user.getExtension().getExtensions().get(ENTERPRISE_URN);
                        if (ext instanceof EnterpriseUserExtension enterpriseExt) {
                                enterpriseData = new HashMap<>();
                                enterpriseData.put("employeeNumber", enterpriseExt.getEmployeeNumber());
                                enterpriseData.put("department", enterpriseExt.getDepartment());
                                enterpriseData.put("costCenter", enterpriseExt.getCostCenter());
                                enterpriseData.put("organization", enterpriseExt.getOrganization());
                                enterpriseData.put("division", enterpriseExt.getDivision());
                        }
                }

                // Map Generic Extensions (if any custom ones exist)
                // Note: For now, we only explicitly support Enterprise extension in the
                // strong-typed response field.

                return ScimUserResponse.builder()
                                .schemas(List.of("urn:ietf:params:scim:schemas:core:2.0:User", ENTERPRISE_URN))
                                .id(String.valueOf(user.getId()))
                                .externalId(user.getExternalId())
                                .userName(user.getUserName())
                                .name(new ScimUserResponse.Name(
                                                user.getFamilyName(),
                                                user.getGivenName(),
                                                user.getFormattedName()))
                                .title(user.getTitle())
                                .emails(List.of(new ScimUserResponse.Email(user.getUserName(), true))) // Map userName
                                                                                                       // to email as
                                                                                                       // per
                                                                                                       // plan
                                .active(user.isActive())
                                .enterpriseExtension(enterpriseData)
                                .meta(new ScimUserResponse.Meta(
                                                "User",
                                                user.getCreated() != null
                                                                ? user.getCreated()
                                                                                .format(DateTimeFormatter.ISO_DATE_TIME)
                                                                : null,
                                                user.getLastModified() != null
                                                                ? user.getLastModified()
                                                                                .format(DateTimeFormatter.ISO_DATE_TIME)
                                                                : null,
                                                "/scim/v2/Users/" + user.getId()))
                                .build();
        }
}
