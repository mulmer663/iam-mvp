package com.iam.registry.application.scim;

import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.IamBusinessException;
import com.iam.registry.domain.scim.ScimDynamicResource;
import com.iam.registry.domain.scim.ScimDynamicResourceRepository;
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

    @Transactional(readOnly = true)
    public Map<String, Object> listResources(String resourceType) {
        List<Map<String, Object>> items = resourceRepository.findAllByResourceType(resourceType)
                .stream().map(this::toScimResponse).toList();
        return Map.of(
                "schemas", List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
                "totalResults", items.size(),
                "startIndex", 1,
                "itemsPerPage", items.size(),
                "Resources", items);
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
        // TODO: Validate attributes using IamAttributeMeta (simplified for now)

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
