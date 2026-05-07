package com.iam.registry.application.scim;

import com.iam.registry.application.scim.filter.DynamicResourceFilterQuery;
import com.iam.registry.application.scim.filter.ScimFilterParser;
import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.IamBusinessException;
import com.iam.registry.domain.scim.ScimDynamicResource;
import com.iam.registry.domain.scim.ScimDynamicResourceRepository;
import com.unboundid.scim2.common.filters.Filter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScimDynamicResourceService {

    private final ScimDynamicResourceRepository resourceRepository;
    private final ScimFilterParser filterParser;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public Map<String, Object> listResources(String resourceType, ScimSearchRequest request) {
        Filter filter = filterParser.parse(request.filter());

        String baseWhere = "WHERE r.resource_type = :resourceType";
        Map<String, Object> params = new HashMap<>();
        params.put("resourceType", resourceType);

        if (filter != null) {
            // DynamicResourceFilterQuery는 상태를 가지므로 매 호출마다 새 인스턴스 사용
            DynamicResourceFilterQuery queryBuilder = new DynamicResourceFilterQuery();
            Map<String, Object> filterParams = queryBuilder.build(filter);
            baseWhere += " AND " + queryBuilder.getWhereClause();
            params.putAll(filterParams);
        }

        // totalResults 조회
        String countSql = "SELECT COUNT(*) FROM scim_dynamic_resource r " + baseWhere;
        Query countQuery = entityManager.createNativeQuery(countSql);
        params.forEach(countQuery::setParameter);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        // count=0: 카운트만 반환
        if (request.isCountOnly()) {
            return buildListResponse(List.of(), (int) total, request.startIndex(), 0);
        }

        // 리소스 조회
        String dataSql = "SELECT r.id, r.scim_id, r.resource_type, r.external_id, "
                + "r.attributes, r.created_at, r.last_modified, r.version "
                + "FROM scim_dynamic_resource r "
                + baseWhere
                + " ORDER BY r.created_at DESC, r.id ASC"
                + " LIMIT :limit OFFSET :offset";

        Query dataQuery = entityManager.createNativeQuery(dataSql, ScimDynamicResource.class);
        params.forEach(dataQuery::setParameter);
        dataQuery.setParameter("limit", request.count());
        dataQuery.setParameter("offset", request.offset());

        @SuppressWarnings("unchecked")
        List<ScimDynamicResource> resources = dataQuery.getResultList();
        List<Map<String, Object>> items = resources.stream().map(this::toScimResponse).toList();

        return buildListResponse(items, (int) total, request.startIndex(), items.size());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getResource(String resourceType, String scimId) {
        ScimDynamicResource resource = resourceRepository.findByScimIdAndResourceType(scimId, resourceType)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        UUID.randomUUID().toString().substring(0, 8), "Resource not found: " + scimId));
        return toScimResponse(resource);
    }

    @Transactional
    public Map<String, Object> createResource(String resourceType, Map<String, Object> payload) {
        String scimId = (String) payload.get("id");
        if (scimId == null) {
            scimId = UUID.randomUUID().toString();
        }

        String externalId = (String) payload.get("externalId");

        ScimDynamicResource resource = ScimDynamicResource.builder()
                .scimId(scimId)
                .resourceType(resourceType)
                .externalId(externalId)
                .attributes(payload)
                .build();

        resourceRepository.save(resource);
        log.info("Created dynamic resource: {}/{} (PK: {})", resourceType, scimId, resource.getId());

        return toScimResponse(resource);
    }

    @Transactional
    public Map<String, Object> updateResource(String resourceType, String scimId, Map<String, Object> payload) {
        ScimDynamicResource resource = resourceRepository.findByScimIdAndResourceType(scimId, resourceType)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        UUID.randomUUID().toString().substring(0, 8), "Resource not found: " + scimId));

        resource.setExternalId((String) payload.get("externalId"));
        resource.setAttributes(payload);

        resourceRepository.save(resource);
        log.info("Updated dynamic resource: {}/{}", resourceType, scimId);

        return toScimResponse(resource);
    }

    @Transactional
    public void deleteResource(String resourceType, String scimId) {
        ScimDynamicResource resource = resourceRepository.findByScimIdAndResourceType(scimId, resourceType)
                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        UUID.randomUUID().toString().substring(0, 8), "Resource not found: " + scimId));
        resourceRepository.delete(resource);
        log.info("Deleted dynamic resource: {}/{}", resourceType, scimId);
    }

    private Map<String, Object> buildListResponse(
            List<Map<String, Object>> items, int total, int startIndex, int itemsPerPage) {
        return Map.of(
                "schemas",      List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
                "totalResults", total,
                "startIndex",   startIndex,
                "itemsPerPage", itemsPerPage,
                "Resources",    items);
    }

    private Map<String, Object> toScimResponse(ScimDynamicResource resource) {
        Map<String, Object> response = new HashMap<>(resource.getAttributes());
        response.put("id", resource.getScimId());

        Map<String, Object> meta = new HashMap<>();
        meta.put("resourceType", resource.getResourceType());
        meta.put("created", resource.getCreatedAt());
        meta.put("lastModified", resource.getLastModified());
        meta.put("version", "W/\"" + (resource.getVersion() != null ? resource.getVersion() : 0) + "\"");
        meta.put("location", "/scim/v2/" + resource.getResourceType() + "s/" + resource.getScimId());

        response.put("meta", meta);
        return response;
    }
}
