package com.iam.registry.config.init;

import com.iam.registry.application.scim.ScimResourceService;
import com.iam.registry.domain.user.IamUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
@Profile({ "local", "dev", "test", "default" })
public class SampleUserInitializer implements CommandLineRunner {

    private final IamUserRepository iamUserRepository;
    private final ScimResourceService scimResourceService;

    private static final String ENTERPRISE_URN =
            "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

    @Override
    public void run(String... args) {
        if (iamUserRepository.count() > 0) {
            return;
        }
        log.info("[SampleUserInitializer] Seeding sample users...");
        SAMPLE_USERS.forEach(scimResourceService::createUser);
        log.info("[SampleUserInitializer] Seeded {} sample users.", SAMPLE_USERS.size());
    }

    private static final List<Map<String, Object>> SAMPLE_USERS = List.of(
            user("michael.kim", "Kim", "Michael", "IT Director", true,
                    "michael.kim@corp.example", "EMP-0001", "GLOBAL-IT", "FTE"),
            user("jane.lee", "Lee", "Jane", "Security Engineer", true,
                    "jane.lee@corp.example", "EMP-0002", "IT-SEC", "FTE"),
            user("tom.park", "Park", "Tom", "Infrastructure Lead", true,
                    "tom.park@corp.example", "EMP-0003", "IT-INFRA", "FTE"),
            user("sarah.choi", "Choi", "Sarah", "Business Analyst", true,
                    "sarah.choi@corp.example", "EMP-0004", "IT-APPS", "FTE"),
            user("james.yoon", "Yoon", "James", "DevOps Engineer", true,
                    "james.yoon@corp.example", "EMP-0005", "IT-INFRA", "FTE"),
            user("emily.han", "Han", "Emily", "IAM Engineer", true,
                    "emily.han@corp.example", "EMP-0006", "IT-SEC", "FTE"),
            user("david.jung", "Jung", "David", "Internal Auditor", true,
                    "david.jung@corp.example", "EMP-0007", "AUDIT-01", "FTE"),
            user("lisa.oh", "Oh", "Lisa", "SAP HR Specialist", true,
                    "lisa.oh@corp.example", "EMP-0008", "HR-DIV", "FTE"),
            user("kevin.shin", "Shin", "Kevin", "Security Operations", true,
                    "kevin.shin@corp.example", "EMP-0009", "IT-SECOPS", "FTE"),
            user("grace.lim", "Lim", "Grace", "Vendor Manager", false,
                    "grace.lim@vendor.example", "VND-0001", "EXT-VND", "CONTRACTOR")
    );

    private static Map<String, Object> user(
            String userName, String familyName, String givenName,
            String title, boolean active, String email,
            String employeeNumber, String department, String userType) {
        return Map.of(
                "schemas", List.of(
                        "urn:ietf:params:scim:schemas:core:2.0:User",
                        ENTERPRISE_URN),
                "userName", userName,
                "name", Map.of("familyName", familyName, "givenName", givenName),
                "title", title,
                "active", active,
                "userType", userType,
                "emails", List.of(Map.of("value", email, "type", "work", "primary", true)),
                ENTERPRISE_URN, Map.of(
                        "employeeNumber", employeeNumber,
                        "department", department)
        );
    }
}
