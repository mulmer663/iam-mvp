package com.iam.registry.application.user;

import com.iam.registry.domain.user.IamUser;
import com.iam.registry.domain.user.IamUserRepository;
import com.iam.registry.domain.user.IdentityLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resolves an external (system, externalId) pair to an {@link IamUser},
 * preferring an explicit IdentityLink mapping and falling back to a
 * direct externalId match on the user record.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityCorrelationService {

    private final IdentityLinkRepository identityLinkRepository;
    private final IamUserRepository iamUserRepository;

    @Transactional(readOnly = true)
    public Optional<IamUser> correlate(String systemId, String externalId) {
        log.debug("Correlating identity: systemId={}, externalId={}", systemId, externalId);

        Optional<IamUser> userFromLink = identityLinkRepository.findBySystemTypeAndExternalId(systemId, externalId)
                .flatMap(link -> iamUserRepository.findById(link.getIamUserId()));

        if (userFromLink.isPresent()) {
            return userFromLink;
        }

        log.debug("No IdentityLink found. Checking IamUser directly by externalId: {}", externalId);
        return iamUserRepository.findByExternalId(externalId);
    }
}
