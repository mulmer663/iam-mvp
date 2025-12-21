MVP 목표: "HR 원천 데이터 수신 -> IAM 내부 사용자 생성(Core+Ext) -> 식별자 분리 -> 대상 시스템(Dummy)으로 프로비저닝 메시지 발행"

# 📜 IAM MVP 기술 명세서 (Version 1.0)
## 1. 시스템 아키텍처 및 데이터 흐름도
Spring Boot 애플리케이션을 Core와 Connector로 논리적으로 나누거나, 물리적으로 분리할 수 있습니다. MVP에서는 멀티 모듈 프로젝트 구성을 권장합니다.

graph LR
    subgraph "External"
        HR[HR System (Mock)]
        AD[Active Directory (Mock)]
    end

    subgraph "IAM System"
        direction TB
        
        subgraph "Ingestion Layer"
            HRC[HR Connector]
        end
        
        MQ1((RabbitMQ: iam.ingest))
        
        subgraph "Core Domain"
            Logic[Sync Service]
            DB[(PostgreSQL)]
        end
        
        MQ2((RabbitMQ: iam.provision))
        
        subgraph "Provisioning Layer"
            ADC[AD Connector]
        end
    end

    HR -->|1. CSV/JSON| HRC
    HRC -->|2. Event: user.raw| MQ1
    MQ1 -->|3. Consume| Logic
    Logic <-->|4. Read/Write| DB
    Logic -->|5. Command: create.account| MQ2
    MQ2 -->|6. Consume| ADC
    ADC -->|7. LDAP Call| AD


## 2. 데이터베이스 스키마 (PostgreSQL & JPA)
MVP의 핵심인 Core/Extension 분리와 **식별자 분리(Identity Link)**를 위한 엔티티 명세입니다.

### 2.1. ERD 요약
iam_user: IAM 내부 고유 사용자 (불변)
iam_user_extension: 사용자의 가변 속성 (JSONB)
identity_link: 외부 시스템 ID와 내부 UUID의 매핑 테이블 (추적 난독화 핵심)
event_log: 변경의 원인과 결과를 기록

###2.2. JPA Entity 명세 (Java)
AI에게 이 코드를 기반으로 엔티티를 생성하라고 지시하세요.
```
// 1. Core User (고정 속성)
@Entity
@Table(name = "iam_user")
public class IamUser {
    @Id
    @Column(name = "user_id", length = 36)
    private String id; // UUID

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId; // IAM 로그인용 ID

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private UserStatus status; // ACTIVE, INACTIVE

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private IamUserExtension extension;
    
    // CreatedAt, UpdatedAt (Audit)
}
```

```
// 2. Extension (가변 속성 - JSONB)
@Entity
@Table(name = "iam_user_extension")
public class IamUserExtension {
    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private IamUser user;

    // MVP 단계에서는 Map<String, Object> 사용
    // DB Column Definition: jsonb
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();
}
```

```
// 3. Identity Link (식별자 연결 고리)
@Entity
@Table(name = "identity_link", indexes = {
    @Index(name = "idx_link_external", columnList = "system_type, external_id")
})
public class IdentityLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iam_user_id", nullable = false)
    private String iamUserId; // FK (논리적 연결)

    @Column(name = "system_type", nullable = false)
    private String systemType; // "HR", "AD", "SAP"

    @Column(name = "external_id", nullable = false)
    private String externalId; // 예: 사번 "2023001", AD "hong.g"

    // 이 계정의 현재 상태 (삭제되어도 이력 유지를 위해 Row는 남김)
    @Column(name = "is_active")
    private boolean active;
}
```

## 3. 메시지 큐 프로토콜 (RabbitMQ)
서비스 간 통신 규약입니다. 바이브 코딩 시 **"이 JSON 포맷으로 메시지를 발행/수신하는 코드를 짜줘"**라고 해야 합니다.

### 3.1. Exchange & Queue 설정
Exchange: iam.topic (Topic Type)
Queue 1: q.iam.core.ingest (Binding: hr.event.#)
Queue 2: q.iam.connector.ad (Binding: cmd.ad.#)

### 3.2. Payload 명세

A. 수집 이벤트 (HR Connector -> Core) Routing Key: hr.event.user.sync
```
{
  "traceId": "uuid-gen-by-connector",
  "eventType": "SYNC_USER",
  "timestamp": "2024-01-01T10:00:00",
  "payload": {
    "hrEmpId": "2024001",    // 외부 식별자
    "name": "Hong Gildong",  // Core 속성
    "attributes": {          // 확장 속성
      "deptCode": "DEV01",
      "position": "Senior",
      "email": "hong@test.com",
      "ext_01": "Something"
    }
  }
}
```

B. 프로비저닝 명령 (Core -> AD Connector) Routing Key: cmd.ad.user.create
```
{
  "traceId": "uuid-passed-from-ingest",
  "causeEventId": "evt-001", // DB event_log ID
  "command": "CREATE_ACCOUNT",
  "payload": {
    "targetSystemId": "hong.g", // AD 계정명 (IdentityLink에서 조회 혹은 생성된 값)
    "attributes": {
      "cn": "Hong Gildong",
      "department": "DEV01",
      "description": "Created by IAM"
    }
  }
}
```

## 4. 핵심 비즈니스 로직 (Core Service)
이 로직 흐름을 AI에게 주입하여 UserService.java를 작성하게 하세요.

알고리즘: processHrSync(UserSyncEvent event)
1. 식별자 조회:
    - IdentityLink 테이블에서 systemType="HR" AND externalId=event.hrEmpId로 조회.
2. 분기 (신규 vs 기존):
    - Case A (결과 없음 - 신규 입사):
        1. 새로운 UUID 생성 (newUuid).
        2. IamUser 생성 및 저장 (Core 속성 매핑).
        3. IamUserExtension 생성 및 저장 (event.attributes를 jsonb로 저장).
        4. IdentityLink 생성 및 저장 (HR ID <-> newUuid).
        5. ProvisioningService 호출 -> AD 계정 생성 규칙에 따라 IdentityLink (AD용) 미리 생성 -> RabbitMQ로 CREATE 명령 발행.
    - Case B (결과 있음 - 정보 갱신):
        1. IamUser 및 Extension 업데이트 (Dirty Checking).
        2. 변경 사항이 감지되면 RabbitMQ로 UPDATE 명령 발행.

## 5. MVP 개발을 위한 바이브 코딩 프롬프트 가이드
이제 위 명세를 바탕으로 코드를 생성할 때 사용할 프롬프트 예시입니다.

### Step 1: 엔티티 생성

"Spring Boot 3.5.8, Java 21, JPA 환경이야. 첨부한 '2.2 JPA Entity 명세'를 참고해서 IamUser, IamUserExtension, IdentityLink 엔티티 클래스를 작성해줘. Extension의 attributes는 Map<String, Object>로 하고 Postgres jsonb 타입을 써야 해."

### Step 2: 리포지토리 및 서비스 껍데기

"위 엔티티에 대한 JpaRepository를 만들고, IdentityLink를 통해 HR ID로 내부 User를 찾는 메서드를 포함해줘. 그리고 UserSyncService 클래스를 만들어서 RabbitMQ 리스너 구조를 잡아줘."

### Step 3: 로직 구현

"HR에서 수신된 JSON 데이터가 다음과 같을 때(3.2 Payload A 참고), UserSyncService에서 신규 사용자면 IamUser와 IdentityLink를 생성하고, 기존 사용자면 정보를 업데이트하는 로직을 작성해줘. @Transactional 안에서 동작해야 해."

### Step 4: 프로비저닝 발행

"사용자가 저장된 후, AD로 계정 생성 명령을 보내야 해. RabbitStreamBridge를 사용해서 3.2 Payload B 포맷으로 메시지를 발행하는 코드를 추가해줘."

### 💡 Genius Note: Map<String, Object>에 대한 조언

MVP에서는 Map<String, Object>로 가되, 추후 확장을 위해 Converter 하나만 딱 만들어 두세요.

```
// MVP 팁: 속성을 꺼낼 때 매번 캐스팅하지 않도록 유틸리티 메서드 사용
public class AttributeUtils {
    public static String getString(Map<String, Object> attrs, String key) {
        return attrs.getOrDefault(key, "").toString();
    }
    
    public static Integer getInt(Map<String, Object> attrs, String key) {
        // 안전한 타입 변환 로직
    }
}
```