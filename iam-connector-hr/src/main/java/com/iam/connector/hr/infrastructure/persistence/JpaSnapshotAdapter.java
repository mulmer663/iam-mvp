package com.iam.connector.hr.infrastructure.persistence;

import com.iam.connector.hr.application.port.out.SnapshotPort;
import com.iam.connector.hr.infrastructure.persistence.entity.HrSnapshot;
import com.iam.connector.hr.infrastructure.persistence.repository.HrSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JpaSnapshotAdapter implements SnapshotPort {

    private final HrSnapshotRepository repository;

    @Override
    public Optional<String> getHash(String externalId) {
        return repository.findById(externalId).map(HrSnapshot::getHash);
    }

    @Override
    @Transactional
    public void save(String externalId, String hash, String systemId) {
        HrSnapshot snapshot = repository.findById(externalId)
                .orElse(HrSnapshot.builder().externalId(externalId).systemId(systemId).build());

        // Update previous hash for rollback capability
        if (snapshot.getHash() != null) {
            snapshot.setPreviousHash(snapshot.getHash());
        }

        snapshot.setHash(hash);
        snapshot.setSystemId(systemId); // Ensure systemId is set correctly in updates
        repository.save(snapshot);
    }

    @Override
    @Transactional
    public void delete(String externalId) {
        repository.deleteById(externalId);
    }

    @Override
    @Transactional
    public void revert(String externalId) {
        repository.findById(externalId).ifPresent(snapshot -> {
            if (snapshot.getPreviousHash() != null) {
                // Revert to previous hash
                snapshot.setHash(snapshot.getPreviousHash());
                snapshot.setPreviousHash(null);
                repository.save(snapshot);
            } else {
                // No previous hash means this was a new record -> Delete it
                repository.delete(snapshot);
            }
        });
    }

    @Override
    public Set<String> getAllExternalIds(String systemId) {
        return repository.findBySystemId(systemId).stream()
                .map(HrSnapshot::getExternalId)
                .collect(Collectors.toSet());
    }
}
