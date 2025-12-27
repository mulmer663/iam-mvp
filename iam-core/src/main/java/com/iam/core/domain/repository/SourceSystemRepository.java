package com.iam.core.domain.repository;

import com.iam.core.domain.entity.SourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SourceSystemRepository extends JpaRepository<SourceSystem, String> {
}
