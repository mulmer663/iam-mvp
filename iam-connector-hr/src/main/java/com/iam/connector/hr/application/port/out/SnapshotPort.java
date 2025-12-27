package com.iam.connector.hr.application.port.out;

import java.util.Optional;
import java.util.Set;

/**
 * Port to manage HR record snapshots (fingerprints).
 */
public interface SnapshotPort {
    Optional<String> getHash(String externalId);

    void save(String externalId, String hash, String systemId);

    void delete(String externalId);

    Set<String> getAllExternalIds(String systemId);
}
