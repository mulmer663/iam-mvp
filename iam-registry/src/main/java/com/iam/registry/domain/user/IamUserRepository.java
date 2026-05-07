package com.iam.registry.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface IamUserRepository extends JpaRepository<IamUser, Long>,
        JpaSpecificationExecutor<IamUser> {

    Optional<IamUser> findByExternalId(String externalId);

    Optional<IamUser> findByUserName(String userName);
}
