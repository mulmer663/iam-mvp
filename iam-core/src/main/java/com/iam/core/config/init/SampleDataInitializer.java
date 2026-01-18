package com.iam.core.config.init;

import com.iam.core.application.dto.UserSyncEvent;
import com.iam.core.application.service.IamUserUpdateService;
import com.iam.core.application.service.SyncHistoryService;
import com.iam.core.domain.constant.AttributeConstants;
import com.iam.core.domain.constant.SystemConstants;
import com.iam.core.domain.entity.IamUser;
import com.iam.core.domain.vo.*;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.iam.core.domain.constant.SyncConstants.*;

/**
 * 샘플 사용자 데이터 이니셜라이저.
 * 개발 및 테스트 환경에서 사용할 풍부한 사용자 데이터를 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
@Profile({ "local", "dev", "test", "default" })
public class SampleDataInitializer implements CommandLineRunner {

    private final IamUserUpdateService iamUserUpdateService;
    private final SyncHistoryService syncHistoryService;
    private final com.iam.core.domain.repository.IamUserRepository iamUserRepository;

    @Override
    public void run(String... args) {
        String traceId = "T-" + TSID.fast().toLong();
        MDC.put(TRACE_ID, traceId);
        MDC.put(OPERATION_TYPE, EVENT_USER_CREATE);

        try {
            if (iamUserRepository.count() > 0) {
                log.info("ℹ️ DB 데이터 존재로 초기화 건너뜜");
                return;
            }

            log.info("🚀 샘플 데이터 생성 시작...");

            // 1. Super Admin (Full Attribute Set)
            createUserViaService("super.admin", "Michael", "Admin", "Michael Admin", "Mike", "IT Director", true,
                    "michael.admin@global-iam.com", "https://profile.global-iam.com/madmin", "Employee",
                    "en-US", "en_US", "Asia/Seoul",
                    "GLOBAL-IT", "ADM001", "CC-100", "Global Tech HQ", "Technology", "EXT-000");

            // 2. Jane Doe (External Auditor)
            IamUser jane = createUserViaService("jane.doe", "Jane", "Doe", "Jane Doe", "Jay", "External Auditor", true,
                    "jane.doe@audit-firm.com", "https://profile.audit-firm.com/jdoe", "Contractor",
                    "en-US", "en_US", "America/New_York",
                    "AUDIT-01", "EXT-101", "CC-900", "External Audit Corp", "Finance", "ADM001");

            // 3. John Smith (Developer)
            createUserViaService("john.smith", "John", "Smith", "John Smith", "Johnny", "Senior Developer", true,
                    "john.smith@tech-corp.com", "https://github.com/jsmith", "Employee",
                    "en-GB", "en_GB", "Europe/London",
                    "DEV-OPS", "DEV-007", "CC-200", "Global Tech UK", "Development", "ADM001");

            log.info("✅ 사용자 데이터 생성 완료");

            // 이력 생성 (Jane Doe 기준)
            createSyncHistoryViaService(jane, traceId);

        } finally {
            MDC.clear();
        }
    }

    private IamUser createUserViaService(String userName, String given, String family, String displayName,
            String nickName,
            String title, boolean active, String email, String profileUrl, String userType,
            String language, String locale, String timezone,
            String dept, String empNo, String costCenter, String org, String division, String managerId) {

        // 맵 구조로 attributes 생성 (Service 입력 규격에 맞춤)
        Map<String, UniversalData> attributes = new HashMap<>();

        // Core Attributes
        attributes.put("userName", new StringData(userName));
        attributes.put("givenName", new StringData(given));
        attributes.put("familyName", new StringData(family));
        attributes.put("displayName", new StringData(displayName));
        attributes.put("nickName", new StringData(nickName));
        attributes.put("title", new StringData(title));
        attributes.put("active", new BooleanData(active));
        attributes.put("email", new StringData(email));
        attributes.put("profileUrl", new StringData(profileUrl));
        attributes.put("userType", new StringData(userType));
        attributes.put("preferredLanguage", new StringData(language));
        attributes.put("locale", new StringData(locale));
        attributes.put("timezone", new StringData(timezone));

        // Use ListData for emails
        var primaryEmail = new HashMap<String, UniversalData>();
        primaryEmail.put("value", new StringData(email));
        primaryEmail.put("type", new StringData("work"));
        primaryEmail.put("primary", new BooleanData(true));
        attributes.put("emails",
                new ListData(List.of(new MapData(primaryEmail))));

        // Enteprise Extension Attributes
        attributes.put("department", new StringData(dept));
        attributes.put("employeeNumber", new StringData(empNo));
        attributes.put("costCenter", new StringData(costCenter));
        attributes.put("organization", new StringData(org));
        attributes.put("division", new StringData(division));
        attributes.put("managerId", new StringData(managerId));

        // Use ListData/MapData for addresses if defined (e.g. simplified 'timezone' as
        // proxy for region)
        // Note: For full Sample Data, we can hardcode an address based on inputs
        if (timezone.contains("Seoul")) {
            var address = new HashMap<String, UniversalData>();
            address.put("type", new StringData("work"));
            address.put("primary", new BooleanData(true));
            address.put("country", new StringData("KR"));
            address.put("region", new StringData("Seoul"));
            address.put("locality", new StringData("Gangnam-gu"));
            address.put("postalCode", new StringData("06232"));
            address.put("formatted", new StringData("Teheran-ro, Seoul, Korea"));
            attributes.put("addresses",
                    new ListData(List.of(new MapData(address))));
        } else if (timezone.contains("New_York")) {
            var address = new HashMap<String, UniversalData>();
            address.put("type", new StringData("work"));
            address.put("primary", new BooleanData(true));
            address.put("country", new StringData("US"));
            address.put("region", new StringData("New York"));
            address.put("locality", new StringData("New York"));
            address.put("postalCode", new StringData("10001"));
            address.put("formatted", new StringData("5th Ave, New York, NY"));
            attributes.put("addresses",
                    new ListData(List.of(new MapData(address))));
        }

        // Phone Numbers
        var mobilePhone = new HashMap<String, UniversalData>();
        mobilePhone.put("value", new StringData("010-1234-5678"));
        mobilePhone.put("type", new StringData("mobile"));

        var workPhone = new HashMap<String, UniversalData>();
        workPhone.put("value", new StringData("02-123-4567"));
        workPhone.put("type", new StringData("work"));

        attributes.put("phoneNumbers",
                new ListData(List.of(new MapData(mobilePhone), new MapData(workPhone))));

        // Additional Multi-valued Attributes

        // IMS
        var im = new HashMap<String, UniversalData>();
        im.put("value", new StringData(email));
        im.put("type", new StringData("xmpp"));
        attributes.put("ims", new ListData(List.of(new MapData(im))));

        // Photos
        var photo = new HashMap<String, UniversalData>();
        photo.put("value", new StringData("https://example.com/photos/" + empNo + ".jpg"));
        photo.put("type", new StringData("photo"));
        attributes.put("photos",
                new ListData(List.of(new MapData(photo))));

        // Groups
        var group = new HashMap<String, UniversalData>();
        group.put("value", new StringData("group-123")); // simple value for now
        group.put("display", new StringData("Employees"));
        attributes.put("groups",
                new ListData(List.of(new MapData(group))));

        // Entitlements
        var entitlement = new HashMap<String, UniversalData>();
        entitlement.put("value", new StringData("urn:entitlement:1"));
        attributes.put("entitlements",
                new ListData(List.of(new MapData(entitlement))));

        // Roles
        var role = new HashMap<String, UniversalData>();
        role.put("value", new StringData("User"));
        attributes.put("roles", new ListData(List.of(new MapData(role))));

        // x509Certificates
        var cert = new HashMap<String, UniversalData>();
        cert.put("value", new StringData("MIIDBjCC...")); // truncated cert
        attributes.put("x509Certificates",
                new ListData(List.of(new MapData(cert))));

        return iamUserUpdateService.create(empNo, attributes);
    }

    private void createSyncHistoryViaService(IamUser user, String traceId) {
        String systemId = "SAP_HR";

        // 1. 실제 런타임과 동일한 UserSyncEvent 전문(Full Object) 생성
        Map<String, Object> payload = new HashMap<>();
        payload.put("empNo", "EXT-101");
        payload.put("email", user.getUserName());
        payload.put("status", "ACTIVE");
        payload.put("firstName", user.getGivenName());
        payload.put("lastName", user.getFamilyName());

        UserSyncEvent mockEvent = new UserSyncEvent(
                traceId,
                systemId,
                EVENT_USER_CREATE, // "USER_CREATE"
                LocalDateTime.now(), // timestamp
                payload // payload
        );

        // 2. 결과 데이터 시뮬레이션
        Map<String, Object> resultSnapshot = new HashMap<>();
        resultSnapshot.put(AttributeConstants.SYNC_TYPE, EVENT_USER_CREATE);
        resultSnapshot.put("status", "CREATED");

        // 3. Service 호출하여 이력 기록
        syncHistoryService.logSuccess(
                traceId,
                DIRECTION_RECON,
                EVENT_USER_CREATE,
                user.getUserName(),
                user.getId(),
                systemId,
                SystemConstants.SYSTEM_IAM,
                resultSnapshot,
                "User created via SampleDataInitializer",
                null, // parentId
                Map.of("event", mockEvent),
                1L, // userRevId
                1L // ruleRevId
        );
    }
}
