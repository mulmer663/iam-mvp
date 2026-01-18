package com.iam.core.domain.repository;

import com.iam.core.domain.entity.ScimSchemaMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScimSchemaMetaRepository extends JpaRepository<ScimSchemaMeta, String> {
}
