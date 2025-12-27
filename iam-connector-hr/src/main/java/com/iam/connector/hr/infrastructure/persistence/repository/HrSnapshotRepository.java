package com.iam.connector.hr.infrastructure.persistence.repository;

import com.iam.connector.hr.infrastructure.persistence.entity.HrSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrSnapshotRepository extends JpaRepository<HrSnapshot, String> {
    List<HrSnapshot> findBySystemId(String systemId);
}
