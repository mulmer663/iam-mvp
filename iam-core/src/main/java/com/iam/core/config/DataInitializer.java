package com.iam.core.config;

import com.iam.core.domain.entity.EnterpriseUserExtension;
import com.iam.core.domain.entity.IamUser;
import com.iam.core.domain.entity.IamUserExtension;
import com.iam.core.domain.repository.IamUserRepository;

import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 로컬 개발 환경용 초기 데이터 이니셜라이저.
 * iam-ui의 mocks/data.ts와 동일한 데이터를 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile({ "local", "dev" }) // 운영 환경에서는 실행되지 않도록 제한
public class DataInitializer implements CommandLineRunner {

    private final IamUserRepository iamUserRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (iamUserRepository.count() > 0) {
            log.info("ℹ️ DB에 이미 데이터가 존재하여 초기화를 건너뜁니다.");
            return;
        }

        log.info("🚀 [DataInitializer] IamUser 샘플 데이터 생성을 시작합니다...");

        List<IamUser> users = new ArrayList<>();

        // User 1: Admin
        users.add(createUser(
                "admin", "System", "Administrator", "Manager", true, "admin@iam.com",
                "DEPT01", null, null));

        // User 2: Hong Gildong
        users.add(createUser(
                "hong.g", "Hong", "Gildong", "Principal Engineer", true, "hong@test.com",
                "DEPT01-1", "H001", null));

        // User 3: Kim Free
        users.add(createUser(
                "kim.f", "Kim", "Free", "Junior Engineer", true, "kim@iam.com",
                "DEPT01-2", null, null));

        // User 4: Lee Planner
        users.add(createUser(
                "lee.p", "Lee", "Planner", "Associate", false, "lee@iam.com",
                "DEPT02-1", null, null));

        iamUserRepository.saveAll(users);
        log.info("✅ [DataInitializer] 총 {}건의 사용자 데이터를 생성했습니다.", users.size());
    }

    private IamUser createUser(String userName, String familyName, String givenName,
            String title, boolean active, String email,
            String dept, String empNo, String costCenter) {

        IamUser user = new IamUser();
        user.setUserName(userName);
        user.setFamilyName(familyName);
        user.setGivenName(givenName);
        user.setFormattedName(givenName + " " + familyName);
        user.setTitle(title);
        user.setActive(active);
        user.setCreated(LocalDateTime.now());
        user.setLastModified(LocalDateTime.now());

        // Extension Setup
        IamUserExtension extension = new IamUserExtension();
        extension.setUser(user);

        // Required Schemas
        List<String> schemas = new ArrayList<>();
        schemas.add("urn:ietf:params:scim:schemas:core:2.0:User");

        // Enterprise Extension
        EnterpriseUserExtension enterpriseExt = new EnterpriseUserExtension();
        enterpriseExt.setDepartment(dept);
        enterpriseExt.setEmployeeNumber(empNo);
        enterpriseExt.setCostCenter(costCenter);
        // ... set other fields if necessary

        extension.getExtensions().put("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User", enterpriseExt);
        schemas.add("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");

        extension.setSchemas(schemas);
        user.setExtension(extension);

        return user;
    }
}
