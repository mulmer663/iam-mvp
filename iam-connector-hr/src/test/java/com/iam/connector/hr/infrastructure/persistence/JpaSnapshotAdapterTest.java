package com.iam.connector.hr.infrastructure.persistence;

import com.iam.connector.hr.infrastructure.persistence.entity.HrSnapshot;
import com.iam.connector.hr.infrastructure.persistence.repository.HrSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaSnapshotAdapterTest {

    @Mock
    private HrSnapshotRepository repository;

    @InjectMocks
    private JpaSnapshotAdapter adapter;

    @Test
    @DisplayName("save() should backup existing hash to previousHash")
    void save_ShouldBackupHash() {
        // Given
        String externalId = "emp001";
        String oldHash = "hash_v1";
        String newHash = "hash_v2";
        String system = "HR";

        HrSnapshot existing = HrSnapshot.builder()
                .externalId(externalId)
                .hash(oldHash)
                .systemId(system)
                .build();

        when(repository.findById(externalId)).thenReturn(Optional.of(existing));

        // When
        adapter.save(externalId, newHash, system);

        // Then
        verify(repository).save(argThat(snapshot -> snapshot.getHash().equals(newHash) &&
                snapshot.getPreviousHash().equals(oldHash)));
    }

    @Test
    @DisplayName("revert() should restore previousHash if it exists")
    void revert_ShouldRestorePreviousHash() {
        // Given
        String externalId = "emp001";
        String currentHash = "hash_v2";
        String prevHash = "hash_v1";

        HrSnapshot snapshot = HrSnapshot.builder()
                .externalId(externalId)
                .hash(currentHash)
                .previousHash(prevHash)
                .systemId("HR")
                .build();

        when(repository.findById(externalId)).thenReturn(Optional.of(snapshot));

        // When
        adapter.revert(externalId);

        // Then
        verify(repository).save(argThat(s -> s.getHash().equals(prevHash) &&
                s.getPreviousHash() == null));
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("revert() should delete snapshot if previousHash is null (New record case)")
    void revert_ShouldDeleteIfNoPreviousHash() {
        // Given
        String externalId = "emp002";
        String currentHash = "hash_v1";

        HrSnapshot snapshot = HrSnapshot.builder()
                .externalId(externalId)
                .hash(currentHash)
                .previousHash(null) // New record
                .systemId("HR")
                .build();

        when(repository.findById(externalId)).thenReturn(Optional.of(snapshot));

        // When
        adapter.revert(externalId);

        // Then
        verify(repository).delete(snapshot);
        verify(repository, never()).save(any());
    }
}
