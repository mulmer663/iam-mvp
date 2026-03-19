package com.iam.registry.domain.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransFieldMappingRepository extends JpaRepository<TransFieldMapping, Long> {
    List<TransFieldMapping> findByRuleId(String ruleId);
}
