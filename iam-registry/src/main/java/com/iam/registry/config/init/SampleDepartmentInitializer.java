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
            dept("GLOBAL-IT",  "Global IT",             null,        "EXT-ORG-001", true),
            dept("IT-INFRA",   "Infrastructure",        "GLOBAL-IT", "EXT-ORG-002", true),
            dept("IT-SEC",     "Cyber Security",        "GLOBAL-IT", "EXT-ORG-003", true),
            dept("IT-SECOPS",  "Security Operations",   "IT-SEC",    "EXT-ORG-004", true),
            dept("IT-APPS",    "Business Applications", "GLOBAL-IT", "EXT-ORG-005", true),
            dept("AUDIT-01",   "Internal Audit",        null,        "EXT-ORG-006", true),
            dept("HR-DIV",     "SAP HR Division",       null,        "EXT-ORG-007", true),
            dept("HR-PLAN",    "SAP HR Planning",       "HR-DIV",    "EXT-ORG-008", true),
            dept("HR-OPS",     "SAP HR Operations",     "HR-DIV",    "EXT-ORG-009", true),
            dept("EXT-VND",    "External Vendors",      null,        "EXT-ORG-010", true)
    );

    private static Map<String, Object> dept(String id, String displayName, String parentId,
                                             String externalId, boolean active) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("schemas", List.of(DEPT_SCHEMA));
        payload.put("id", id);
        payload.put("externalId", externalId);
        payload.put("displayName", displayName);
        payload.put("active", active);
        payload.put("parentId", parentId);
        return payload;
    }
}
