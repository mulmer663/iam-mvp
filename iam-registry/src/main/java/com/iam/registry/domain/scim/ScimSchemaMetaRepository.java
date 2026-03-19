package com.iam.registry.domain.scim;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScimSchemaMetaRepository extends JpaRepository<ScimSchemaMeta, String> {
}
