package com.iam.core.domain.repository;

import com.iam.core.domain.entity.SyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyncHistoryRepository extends JpaRepository<SyncHistory, Long> {
    List<SyncHistory> findAllByOrderByCreatedAtDesc();

    List<SyncHistory> findByTraceId(String traceId);

    List<SyncHistory> findByTargetUser(String targetUser);
}
