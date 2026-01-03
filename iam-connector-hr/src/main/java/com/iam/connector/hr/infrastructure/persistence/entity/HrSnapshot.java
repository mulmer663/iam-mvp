package com.iam.connector.hr.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores the fingerprint (hash) of an HR record to detect changes.
 */
@Entity
@Table(name = "HR_CONNECTOR_SNAPSHOT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrSnapshot {

    @Id
    @Column(name = "EXTERNAL_ID", length = 100)
    private String externalId;

    @Column(name = "HASH_VALUE", length = 64, nullable = false)
    private String hash;

    @Column(name = "PREV_HASH_VALUE", length = 64)
    private String previousHash;

    @Column(name = "SYSTEM_ID", length = 50, nullable = false)
    private String systemId;

    @UpdateTimestamp
    @Column(name = "LAST_SEEN")
    private LocalDateTime lastSeen;
}
