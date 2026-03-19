package com.iam.registry.domain.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransRuleMetaRepository extends JpaRepository<TransRuleMeta, String> {
}
