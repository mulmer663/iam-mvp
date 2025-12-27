package com.iam.core.application.service;

import com.iam.core.domain.entity.IamUser;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.IdentityLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service to correlate external identity to an IamUser.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityCorrelationService {

    private final IdentityLinkRepository identityLinkRepository;
    private final IamUserRepository iamUserRepository;

    /**
     * Finds an IamUser based on source system and its external ID.
     *
     * @param systemId   The ID of the source system (e.g., "SAP_HR")
     * @param externalId The ID in that source system
     * @return Optional of found IamUser
     */
    @Transactional(readOnly = true)
    public Optional<IamUser> correlate(String systemId, String externalId) {
        log.debug("Correlating identity: systemId={}, externalId={}", systemId, externalId);

        // 1. Check IdentityLink (Explicit mapping)
        var userFromLink = identityLinkRepository.findBySystemTypeAndExternalId(systemId, externalId)
                .flatMap(link -> iamUserRepository.findById(link.getIamUserId()));

        if (userFromLink.isPresent()) {
            return userFromLink;
        }

        // 2. Fallback: Check IamUser directly by externalId
        log.debug("No IdentityLink found. Checking IamUser directly by externalId: {}", externalId);
        return iamUserRepository.findByExternalId(externalId);
    }
}
