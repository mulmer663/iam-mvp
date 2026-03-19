package com.iam.registry.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IamUserRepository extends JpaRepository<IamUser, Long> {
    Optional<IamUser> findByExternalId(String externalId);

    Optional<IamUser> findByUserName(String userName);
}
