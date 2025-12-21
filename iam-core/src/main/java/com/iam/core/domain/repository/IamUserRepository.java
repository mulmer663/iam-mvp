package com.iam.core.domain.repository;

import com.iam.core.domain.entity.IamUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamUserRepository extends JpaRepository<IamUser, Long> {
}
