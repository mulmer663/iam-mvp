package com.iam.core.domain.repository;

import com.iam.core.domain.entity.TransRuleMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransRuleMetaRepository extends JpaRepository<TransRuleMeta, String> {
}
