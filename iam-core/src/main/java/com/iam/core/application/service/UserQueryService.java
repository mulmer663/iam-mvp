package com.iam.core.application.service;

import com.iam.core.application.dto.ScimListResponse;
import com.iam.core.application.dto.ScimUserResponse;
import com.iam.core.domain.entity.EnterpriseUserExtension;
import com.iam.core.domain.entity.ExtensionData;
import com.iam.core.domain.entity.IamUser;
import com.iam.core.domain.exception.ErrorCode;
import com.iam.core.domain.exception.IamBusinessException;
import com.iam.core.domain.repository.IamUserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
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
        private final EntityManager entityManager;
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

        public ScimUserResponse getUserAtRevision(Long userId, Long revId) {
                AuditReader reader = AuditReaderFactory.get(entityManager);

                // Envers가 이 시점의 IamUser와 연관된 IamUserExtension을 함께 찾아줍니다.
                IamUser auditedUser = reader.find(IamUser.class, userId, revId);

                if (auditedUser == null) {
                        throw new IamBusinessException(
                                ErrorCode.USER_NOT_FOUND,
                                "API",
                                "Revision " + revId + " not found for user: " + userId);
                }

                return toScimUser(auditedUser);
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

        public ScimUserResponse getUserAtTraceId(Long id, String traceId) {
                AuditReader reader = AuditReaderFactory.get(entityManager);

                // 1. getSingleResult 대신 List로 조회하여 예외 방지
                List<Number> revisionIds = entityManager.createQuery(
                                "SELECT cre.id FROM CustomRevisionEntity cre WHERE cre.traceId = :traceId", Number.class)
                        .setParameter("traceId", traceId)
                        .getResultList();

                if (revisionIds.isEmpty()) {
                        // 정의하신 IAM-4103 에러 코드를 사용하여 예외를 던집니다.
                        throw new IamBusinessException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                traceId,
                                "해당 트레이스 ID에 해당하는 리비전이 없습니다.");
                }

                Number revisionNumber = revisionIds.get(0);

                // 2. 찾은 리비전 번호로 당시 엔티티 복원
                IamUser auditedUser = reader.find(IamUser.class, id, revisionNumber);

                if (auditedUser == null) {
                        throw new IamBusinessException(ErrorCode.USER_NOT_FOUND, traceId, "해당 시점에 사용자가 존재하지 않습니다.");
                }

                // 3. 기존에 잘 만들어두신 toScimUser 메서드 활용
                return toScimUser(auditedUser);
        }
}
