package com.iam.core.domain.repository;

import com.iam.core.domain.entity.TransFieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransFieldMappingRepository extends JpaRepository<TransFieldMapping, Long> {
    List<TransFieldMapping> findByRuleId(String ruleId);
}
