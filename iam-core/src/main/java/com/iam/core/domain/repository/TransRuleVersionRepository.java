package com.iam.core.domain.repository;

import com.iam.core.domain.entity.TransRuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransRuleVersionRepository extends JpaRepository<TransRuleVersion, Long> {
    Optional<TransRuleVersion> findByRuleIdAndIsCurrentTrue(String ruleId);
}
