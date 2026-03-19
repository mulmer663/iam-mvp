package com.iam.registry.application;

import com.iam.registry.domain.user.IamUser;
import com.iam.registry.domain.user.IamUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRegistryService {

    private final IamUserRepository iamUserRepository;

    @Transactional
    public IamUser saveUser(IamUser user) {
        return iamUserRepository.save(user);
    }
}
