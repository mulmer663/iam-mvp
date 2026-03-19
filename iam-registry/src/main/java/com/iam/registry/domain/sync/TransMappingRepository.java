package com.iam.registry.domain.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransMappingRepository extends JpaRepository<TransMapping, Long> {
    List<TransMapping> findBySystemIdOrderByExecOrderAsc(String systemId);

    Optional<TransMapping> findBySystemId(String systemId);
}
