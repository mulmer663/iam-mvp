package com.iam.core.domain.repository;

import com.iam.core.domain.entity.TransCodeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransCodeValueRepository extends JpaRepository<TransCodeValue, Long> {
    List<TransCodeValue> findByCodeGroupId(String codeGroupId);
}
