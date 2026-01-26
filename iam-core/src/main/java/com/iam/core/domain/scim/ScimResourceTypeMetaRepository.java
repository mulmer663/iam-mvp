package com.iam.core.domain.scim;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScimResourceTypeMetaRepository extends JpaRepository<ScimResourceTypeMeta, String> {
}
