package com.iam.core.domain.repository;

import com.iam.core.domain.entity.ScimResourceTypeMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScimResourceTypeMetaRepository extends JpaRepository<ScimResourceTypeMeta, String> {
}
