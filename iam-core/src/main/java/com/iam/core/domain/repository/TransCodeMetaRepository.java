package com.iam.core.domain.repository;

import com.iam.core.domain.entity.TransCodeMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransCodeMetaRepository extends JpaRepository<TransCodeMeta, String> {
}
