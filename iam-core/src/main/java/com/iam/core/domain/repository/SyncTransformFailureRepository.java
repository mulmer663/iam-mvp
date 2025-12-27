package com.iam.core.domain.repository;

import com.iam.core.domain.entity.SyncTransformFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncTransformFailureRepository extends JpaRepository<SyncTransformFailure, Long> {
    List<SyncTransformFailure> findByHistoryId(Long historyId);
}
