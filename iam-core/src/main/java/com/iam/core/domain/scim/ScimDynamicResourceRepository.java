package com.iam.core.domain.scim;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScimDynamicResourceRepository extends JpaRepository<ScimDynamicResource, Long> {
    Optional<ScimDynamicResource> findByScimIdAndResourceType(String scimId, String resourceType);

    Optional<ScimDynamicResource> findByExternalIdAndResourceType(String externalId, String resourceType);

    void deleteByScimIdAndResourceType(String scimId, String resourceType);
}
