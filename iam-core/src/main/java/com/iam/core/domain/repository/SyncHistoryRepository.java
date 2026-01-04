package com.iam.core.domain.repository;

import com.iam.core.domain.entity.SyncHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyncHistoryRepository extends JpaRepository<SyncHistory, Long> {
    List<SyncHistory> findAllByOrderByCreatedAtDesc();

    List<SyncHistory> findByTraceId(String traceId);

    Page<SyncHistory> findByTargetUser(String targetUser, Pageable pageable);

    Page<SyncHistory> findByIamUserId(Long iamUserId, Pageable pageable);

    Page<SyncHistory> findByIamUserIdOrTargetUser(Long iamUserId, String targetUser, Pageable pageable);
}
