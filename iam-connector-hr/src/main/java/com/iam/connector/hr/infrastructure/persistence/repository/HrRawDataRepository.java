package com.iam.connector.hr.infrastructure.persistence.repository;

import com.iam.connector.hr.infrastructure.persistence.entity.HrRawData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HrRawDataRepository extends JpaRepository<HrRawData, String> {
}
