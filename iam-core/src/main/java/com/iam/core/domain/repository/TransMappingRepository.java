package com.iam.core.domain.repository;

import com.iam.core.domain.entity.TransMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransMappingRepository extends JpaRepository<TransMapping, Long> {
    List<TransMapping> findBySystemIdOrderByExecOrderAsc(String systemId);
}
