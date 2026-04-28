package com.iam.registry.config.init;

import com.iam.registry.application.scim.ScimDynamicResourceService;
import com.iam.registry.domain.scim.ScimDynamicResourceRepository;
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
@Order(4)
@Profile({ "local", "dev", "test", "default" })
public class SampleDepartmentInitializer implements CommandLineRunner {

    private final ScimDynamicResourceRepository resourceRepository;
    private final ScimDynamicResourceService resourceService;

    private static final String RESOURCE_TYPE = "Department";
    private static final String DEPT_SCHEMA = "urn:iam:params:scim:schemas:2.0:Department";

    @Override
    public void run(String... args) {
        if (resourceRepository.findAllByResourceType(RESOURCE_TYPE).size() > 0) {
            return;
        }
        log.info("[SampleDepartmentInitializer] Seeding sample departments...");
        SAMPLE_DEPARTMENTS.forEach(d -> resourceService.createResource(RESOURCE_TYPE, d));
        log.info("[SampleDepartmentInitializer] Seeded {} sample departments.", SAMPLE_DEPARTMENTS.size());
    }

    private static final List<Map<String, Object>> SAMPLE_DEPARTMENTS = List.of(
            dept("GLOBAL-IT",  "Global IT",             null,        "EXT-ORG-001", true,  "GITD", 0, "EMP-0001", "James Kang",   "CC-1000", "it@corp.local",      "+82-2-0000-1000", "HQ-10F"),
            dept("IT-INFRA",   "Infrastructure",        "GLOBAL-IT", "EXT-ORG-002", true,  "INFRA",1, "EMP-0012", "Minho Yoon",   "CC-1100", "infra@corp.local",   "+82-2-0000-1100", "HQ-9F"),
            dept("IT-SEC",     "Cyber Security",        "GLOBAL-IT", "EXT-ORG-003", true,  "SEC",  1, "EMP-0023", "Jiyeon Park",  "CC-1200", "security@corp.local","+82-2-0000-1200", "HQ-9F"),
            dept("IT-SECOPS",  "Security Operations",   "IT-SEC",    "EXT-ORG-004", true,  "SOPS", 2, "EMP-0031", "Kevin Oh",     "CC-1210", "secops@corp.local",  "+82-2-0000-1210", "HQ-8F"),
            dept("IT-APPS",    "Business Applications", "GLOBAL-IT", "EXT-ORG-005", true,  "APPS", 1, "EMP-0045", "Sora Choi",    "CC-1300", "biz-apps@corp.local","+82-2-0000-1300", "HQ-8F"),
            dept("AUDIT-01",   "Internal Audit",        null,        "EXT-ORG-006", true,  "AUDT", 0, "EMP-0056", "Daniel Lim",   "CC-2000", "audit@corp.local",   "+82-2-0000-2000", "HQ-5F"),
            dept("HR-DIV",     "SAP HR Division",       null,        "EXT-ORG-007", true,  "HRDV", 0, "EMP-0067", "Anna Shin",    "CC-3000", "hr@corp.local",      "+82-2-0000-3000", "HQ-4F"),
            dept("HR-PLAN",    "SAP HR Planning",       "HR-DIV",    "EXT-ORG-008", true,  "HRPL", 1, "EMP-0078", "Brian Jung",   "CC-3100", "hr-plan@corp.local", "+82-2-0000-3100", "HQ-4F"),
            dept("HR-OPS",     "SAP HR Operations",     "HR-DIV",    "EXT-ORG-009", true,  "HROP", 1, "EMP-0089", "Claire Kim",   "CC-3200", "hr-ops@corp.local",  "+82-2-0000-3200", "HQ-4F"),
            dept("EXT-VND",    "External Vendors",      null,        "EXT-ORG-010", true,  "EVND", 0, "EMP-0099", "Tom Han",      "CC-9000", "vendors@corp.local", "+82-2-0000-9000", "HQ-1F")
    );

    private static Map<String, Object> dept(String id, String displayName, String parentId,
                                             String externalId, boolean active,
                                             String deptCode, int level,
                                             String managerId, String managerDisplayName,
                                             String costCenter, String email,
                                             String phoneNumber, String location) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("schemas", List.of(DEPT_SCHEMA));
        payload.put("id", id);
        payload.put("externalId", externalId);
        payload.put("displayName", displayName);
        payload.put("active", active);
        payload.put("parentId", parentId);
        payload.put("deptCode", deptCode);
        payload.put("level", level);
        payload.put("managerId", managerId);
        payload.put("managerDisplayName", managerDisplayName);
        payload.put("costCenter", costCenter);
        payload.put("email", email);
        payload.put("phoneNumber", phoneNumber);
        payload.put("location", location);
        return payload;
    }
}
