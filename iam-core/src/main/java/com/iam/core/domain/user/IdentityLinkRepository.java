package com.iam.core.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentityLinkRepository extends JpaRepository<IdentityLink, Long> {
    Optional<IdentityLink> findBySystemTypeAndExternalId(String systemType, String externalId);
}
