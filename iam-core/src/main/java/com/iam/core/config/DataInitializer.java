package com.iam.core.config;

import com.iam.core.domain.constant.ScimConstants;
import com.iam.core.domain.constant.SyncConstants;
import com.iam.core.domain.constant.SystemConstants;
import com.iam.core.domain.entity.EnterpriseUserExtension;
import com.iam.core.domain.entity.IamUser;
import com.iam.core.domain.entity.IamUserExtension;
import com.iam.core.domain.repository.IamUserRepository;
import com.iam.core.domain.repository.SyncHistoryRepository;
import com.iam.core.domain.entity.*;
import com.iam.core.domain.repository.*;

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
@Profile({ "local", "dev", "test", "default" }) // 운영 환경에서는 실행되지 않도록 제한
public class DataInitializer implements CommandLineRunner {

  private final IamUserRepository iamUserRepository;
  private final SyncHistoryRepository syncHistoryRepository;
  private final TransRuleMetaRepository transRuleMetaRepository;
  private final TransRuleVersionRepository transRuleVersionRepository;
  private final TransMappingRepository transMappingRepository;

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    // Rule Engine Data (Always try to init if mappings are missing)
    initRuleEngineData();

    if (iamUserRepository.count() > 0) {
      log.info("ℹ️ DB에 이미 사용자 데이터가 존재하여 사용자 초기화를 건너뜜...");
      return;
    }

    log.info("🚀 [DataInitializer] 샘플 데이터 생성을 시작합니다...");

    List<IamUser> users = new ArrayList<>();

    // User 1: Super Admin
    users.add(createUser(
        "super.admin", "Michael", "Admin", "IT Director", true, "michael.admin@global-iam.com",
        "GLOBAL-IT", "ADM001", null));

    // User 2: Jane Doe (Auditor)
    IamUser jane = createUser(
        "jane.doe", "Jane", "Doe", "External Auditor", true, "jane.doe@audit-firm.com",
        "AUDIT-01", "EXT-101", null);
    users.add(jane);

    // User 3: John Smith (Security)
    users.add(createUser(
        "john.smith", "John", "Smith", "Security Analyst", true, "john.smith@global-iam.com",
        "SEC-OPS", "SEC-888", null));

    // User 4: Sarah Vendor (Contractor)
    users.add(createUser(
        "sarah.v", "Sarah", "Vendor", "Implementation Partner", false, "sarah.v@partner.com",
        "EXTERNAL-V", "VND-444", null));

    iamUserRepository.saveAll(users);
    log.info("✅ [DataInitializer] 총 {}건의 사용자 데이터를 생성했습니다.", users.size());

    // Sync History Data
    createSyncHistory(jane);
  }

  private void initRuleEngineData() {
    if (transRuleMetaRepository.count() > 0)
      return;

    log.info("🚀 [DataInitializer] 규칙 엔진 초기 데이터 생성을 시작합니다...");

    String ruleId = "SAP_CORE_TRANS";
    TransRuleMeta meta = TransRuleMeta.builder()
        .ruleId(ruleId)
        .ruleName("SAP HR Core Transformation")
        .targetAttribute("CORE")
        .status("ACTIVE")
        .build();
    transRuleMetaRepository.save(meta);

    String script = """
            def res = [:]
            res.userName = source.userName ?: source.empNo?.asString()
            res.familyName = source.familyName?.asString()
            res.givenName = source.givenName?.asString()
            res.title = source.title?.asString()
            res.active = source.active != null ? source.active : new com.iam.core.domain.vo.BooleanData(true)

            // Extension mapping
            res.employeeNumber = source.empNo?.asString()
            res.department = source.deptName?.asString() ?: source.deptCode?.asString()
            return res
        """;

    TransRuleVersion version = TransRuleVersion.builder()
        .ruleId(ruleId)
        .versionNo(1)
        .scriptContent(script)
        .scriptHash("init-hash")
        .isCurrent(true)
        .build();
    transRuleVersionRepository.save(version);

    TransMapping mapping = TransMapping.builder()
        .systemId(SystemConstants.SYSTEM_SAP_HR)
        .ruleId(ruleId)
        .execOrder(1)
        .isMandatory(true)
        .build();
    transMappingRepository.save(mapping);

    log.info("✅ [DataInitializer] {} 규칙 매핑을 생성했습니다.", SystemConstants.SYSTEM_SAP_HR);
  }

  private void createSyncHistory(IamUser user) {
    List<com.iam.core.domain.entity.SyncHistory> histories = new ArrayList<>();
    String traceId = "T-" + TSID.fast().toLong();
    String userId = String.valueOf(user.getId());
    String userName = user.getUserName();

    // 1. HR Sync (Join)
    histories.add(createHistory(traceId, SyncConstants.EVENT_HR_SYNC, SyncConstants.STATUS_SUCCESS, userName,
        "New employee joined from HR",
        """
            {
              "userId": "%s",
              "syncType": "%s",
              "snapshot": {
                "layer": "%s",
                "data": { "empId": "H001", "name": "%s", "position": "Senior Engineer", "dept": "Dev Team" }
              },
              "mappings": [
                { "fromLabel": "%s", "toLabel": "%s", "fromField": "position", "toField": "title", "value": "Senior Engineer" }
              ]
            }
            """
            .formatted(userId, SyncConstants.TYPE_JOIN, SystemConstants.SYSTEM_HR, userName,
                SystemConstants.SYSTEM_HR, SystemConstants.SYSTEM_IAM),
        LocalDateTime.now().minusDays(1)));

    // 2. IAM Core Update
    histories.add(createHistory(traceId, SyncConstants.EVENT_USER_UPDATE, SyncConstants.STATUS_SUCCESS, userName,
        "IAM User record updated",
        """
            {
              "userId": "%s",
              "syncType": "%s",
              "snapshot": {
                "layer": "%s",
                "data": { "id": "%s", "userName": "%s", "active": true }
              }
            }
            """.formatted(userId, SyncConstants.TYPE_JOIN, SystemConstants.SYSTEM_IAM, userId, user.getUserName()),
        LocalDateTime.now().minusDays(1).plusSeconds(5)));

    // 3. AD Provision
    histories.add(createHistory(traceId, SyncConstants.EVENT_AD_PROVISION, SyncConstants.STATUS_SUCCESS, userName,
        "AD Account provisioned",
        """
            {
              "userId": "%s",
              "syncType": "%s",
              "snapshot": {
                "layer": "%s",
                "data": { "sAMAccountName": "%s", "displayName": "%s" }
              },
              "mappings": [
                { "fromLabel": "%s", "toLabel": "%s", "fromField": "title", "toField": "title", "value": "Senior Engineer" }
              ]
            }
            """
            .formatted(userId, SyncConstants.TYPE_JOIN, SystemConstants.SYSTEM_AD, user.getUserName(), userName,
                SystemConstants.SYSTEM_IAM, SystemConstants.SYSTEM_AD),
        LocalDateTime.now().minusDays(1).plusMinutes(1)));

    syncHistoryRepository.saveAll(histories);

    // --- Scenario 2: Promotion (UPDATE_CRITICAL) ---
    List<com.iam.core.domain.entity.SyncHistory> criticalHistories = new ArrayList<>();
    String critTraceId = "T-" + TSID.fast().toLong();

    criticalHistories
        .add(createHistory(critTraceId, SyncConstants.EVENT_HR_SYNC, SyncConstants.STATUS_SUCCESS, userName,
            "User promoted to Principal Engineer",
            """
                {
                  "userId": "%s",
                  "syncType": "%s",
                  "changes": [{ "field": "position", "old": "Senior Engineer", "new": "Principal Engineer" }],
                  "snapshot": {
                    "layer": "%s",
                    "data": { "empId": "H001", "name": "%s", "position": "Principal Engineer", "dept": "Dev Team" }
                  },
                  "mappings": [
                    { "fromLabel": "%s", "toLabel": "%s", "fromField": "position", "toField": "title", "value": "Principal Engineer" }
                  ]
                }
                """
                .formatted(userId, SyncConstants.TYPE_UPDATE_CRITICAL, SystemConstants.SYSTEM_HR, userName,
                    SystemConstants.SYSTEM_HR, SystemConstants.SYSTEM_IAM),
            LocalDateTime.now().minusHours(2)));

    criticalHistories.add(
        createHistory(critTraceId, SyncConstants.EVENT_USER_UPDATE, SyncConstants.STATUS_SUCCESS, userName,
            "IAM title updated",
            """
                {
                  "userId": "%s",
                  "syncType": "%s",
                  "changes": [{ "field": "title", "old": "Senior Engineer", "new": "Principal Engineer" }],
                  "snapshot": {
                    "layer": "%s",
                    "data": { "id": "%s", "title": "Principal Engineer" }
                  }
                }
                """
                .formatted(userId, SyncConstants.TYPE_UPDATE_CRITICAL, SystemConstants.SYSTEM_IAM, userId),
            LocalDateTime.now().minusHours(2).plusSeconds(10)));

    criticalHistories.add(createHistory(critTraceId, SyncConstants.EVENT_AD_PROVISION, SyncConstants.STATUS_SUCCESS,
        userName,
        "AD title provisioned",
        """
            {
              "userId": "%s",
              "syncType": "%s",
              "changes": [{ "field": "title", "old": "Senior Engineer", "new": "Principal Engineer" }],
              "snapshot": {
                "layer": "%s",
                "data": { "sAMAccountName": "%s", "title": "Principal Engineer" }
              },
              "mappings": [
                { "fromLabel": "%s", "toLabel": "%s", "fromField": "title", "toField": "title", "value": "Principal Engineer" }
              ]
            }
            """
            .formatted(userId, SyncConstants.TYPE_UPDATE_CRITICAL, SystemConstants.SYSTEM_AD, user.getUserName(),
                SystemConstants.SYSTEM_IAM, SystemConstants.SYSTEM_AD),
        LocalDateTime.now().minusHours(2).plusMinutes(2)));

    syncHistoryRepository.saveAll(criticalHistories);

    // --- Scenario 3: Large Scale Provisioning (Many Targets) ---
    List<com.iam.core.domain.entity.SyncHistory> provisioningHistories = new ArrayList<>();
    String provTraceId = "T-" + TSID.fast().toLong();
    String[] targets = {
        "AD", "Google Workspace", "Slack", "Zoom", "GitHub",
        "Jira", "Confluence", "AWS", "Azure", "Office 365",
        "Salesforce", "Datadog", "Sentry", "Figma", "Notion",
        "Zendesk", "HubSpot", "Dropbox", "Bitbucket", "PagerDuty"
    };

    // HR & IAM steps for this trace
    provisioningHistories.add(createHistory(provTraceId, SyncConstants.EVENT_HR_SYNC, SyncConstants.STATUS_SUCCESS,
        userName,
        "User attributes updated in HR",
        "{\"syncType\": \"%s\", \"userId\": \"%s\"}".formatted(SyncConstants.TYPE_UPDATE_SIMPLE, userId),
        LocalDateTime.now().minusMinutes(30)));
    provisioningHistories.add(createHistory(provTraceId, SyncConstants.EVENT_USER_UPDATE, SyncConstants.STATUS_SUCCESS,
        userName,
        "IAM Core synchronized",
        "{\"syncType\": \"%s\", \"userId\": \"%s\"}".formatted(SyncConstants.TYPE_UPDATE_SIMPLE, userId),
        LocalDateTime.now().minusMinutes(29)));

    // 20 Provisioning Events
    for (int i = 0; i < targets.length; i++) {
      String targetSystem = targets[i];
      provisioningHistories.add(createHistory(provTraceId, SyncConstants.EVENT_AD_PROVISION,
          SyncConstants.STATUS_SUCCESS, userName,
          "Provisioned to " + targetSystem,
          """
              {
                "userId": "%s",
                "syncType": "%s",
                "targetSystem": "%s",
                "snapshot": {
                  "layer": "TARGET",
                  "data": { "account": "hong.g", "status": "active" }
                },
                "mappings": [
                  { "fromLabel": "%s", "toLabel": "%s", "fromField": "userName", "toField": "account", "value": "hong.g" }
                ]
              }
              """
              .formatted(userId, SyncConstants.TYPE_UPDATE_SIMPLE, targetSystem, SystemConstants.SYSTEM_IAM,
                  targetSystem),
          LocalDateTime.now().minusMinutes(28).plusSeconds(i * 5)));
    }

    syncHistoryRepository.saveAll(provisioningHistories);
    log.info("✅ [DataInitializer] 대규모 프로비저닝(20개 타겟) 시나리오 데이터를 생성했습니다.");
  }

  private com.iam.core.domain.entity.SyncHistory createHistory(String traceId, String type, String status,
      String target, String msg, String payload, LocalDateTime time) {
    return com.iam.core.domain.entity.SyncHistory.builder()
        .traceId(traceId)
        .type(type)
        .status(status)
        .targetUser(target)
        .requestPayload(payload)
        .responsePayload(msg)
        .createdAt(time)
        .build();
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
    schemas.add(ScimConstants.URN_CORE_USER);

    // Enterprise Extension
    EnterpriseUserExtension enterpriseExt = new EnterpriseUserExtension();
    enterpriseExt.setDepartment(dept);
    enterpriseExt.setEmployeeNumber(empNo);
    enterpriseExt.setCostCenter(costCenter);

    extension.getExtensions().put(ScimConstants.URN_ENTERPRISE_USER,
        enterpriseExt);
    schemas.add(ScimConstants.URN_ENTERPRISE_USER);

    extension.setSchemas(schemas);
    user.setExtension(extension);

    return user;
  }
}
