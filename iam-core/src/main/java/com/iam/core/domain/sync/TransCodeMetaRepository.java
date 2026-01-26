package com.iam.core.domain.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransCodeMetaRepository extends JpaRepository<TransCodeMeta, String> {
}
