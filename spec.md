# 📜 IAM MVP 기술 명세서 (Version 1.2 - Hybrid SCIM Storage)

## MVP 목표: "HR 데이터 수집 및 SCIM 2.0 규격 변환 -> IAM 내부 사용자 생성(Core/Extension 분리) -> 식별자 매핑 -> 대상 시스템 프로비저닝"

## 1. 시스템 아키텍처 및 데이터 흐름

SCIM 2.0 프로토콜을 기반으로 외부 시스템(HR)으로부터 데이터를 수신하여 IAM Core에 저장하고, 대상 시스템(AD)으로 프로비저닝합니다.

* **Ingestion:** HR Connector가 원천 데이터를 SCIM User Resource 형태로 변환하여 전송합니다.
* **Core Storage:** 정형 속성(Core)은 컬럼으로, 비정형 속성(Extensions)은 JSONB로 분리 저장하는 하이브리드 방식을 채택합니다.

```mermaid
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

    HR -->|1. Raw Data| HRC
    HRC -->|2. SCIM User Event| MQ1
    MQ1 -->|3. Consume| Logic
    Logic <-->|4. Store Core/Ext| DB
    Logic -->|5. Prov Command| MQ2
    MQ2 -->|6. Consume| ADC
    ADC -->|7. Provision| AD
```

## 2. 데이터베이스 스키마 (PostgreSQL & JPA)

### 2.1. ERD 요약

* **iam_user:** SCIM Core Attributes (id, userName, active, externalId + Flattened Name/Meta).
* **iam_user_extension:** SCIM Extension Attributes (JSONB).
* **iam_user_schema:** 사용자가 준수하는 스키마 목록 (1:N join table).
* **identity_link:** 외부 식별자 매핑 정보.

### 2.2. JPA Entity 명세 (Java 21)

```java
// 1. Core User (Relational Columns for core attributes)
@Entity
@Table(name = "iam_user")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IamUser {
    @Id
    @Column(name = "user_id", length = 36)
    private Long id; // TSID

    @Column(name = "external_id")
    private String externalId; // 원천 시스템 식별자

    @Column(nullable = false, unique = true)
    private String userName;

    // Flattened Name Attributes
    private String familyName;
    private String givenName;
    private String formattedName;

    private boolean active;

    // Meta Attributes (Flat)
    private String resourceType;
    private LocalDateTime created;
    private LocalDateTime lastModified;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private IamUserExtension extension;
}
```

```java
// 2. Extension (Dynamic Attributes - JSONB)
@Entity
@Table(name = "iam_user_extension")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IamUserExtension {
    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private IamUser user;

    // Standard Relational Mapping for schemas
    @ElementCollection
    @CollectionTable(name = "iam_user_schema", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "schema_uri")
    private List<String> schemas = new ArrayList<>();

    // Truly dynamic extensions remain JSONB to avoid schema drift
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, ExtensionData> extensions = new HashMap<>(); 
    // Key: urn:ietf:params:scim:schemas:extension:...
}

/**
 * SCIM Extension의 베이스 클래스.
 * 알려진 스키마(Enterprise 등)는 상속을 통해 타입 안전한 필드를 제공합니다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "urn")
@JsonSubTypes({
    @JsonSubTypes.Type(value = EnterpriseUserExtension.class, name = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User")
})
public abstract class ExtensionData {
}

/**
 * Enterprise User Extension (URN: urn:ietf:params:scim:schemas:extension:enterprise:2.0:User)
 * 정형화된 필드를 제공하여 'Map' 중첩 없이 직접 접근 가능하게 합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnterpriseUserExtension extends ExtensionData {
    private String employeeNumber;
    private String department;
    private String costCenter;
    private String organization;
    private String division;
    // private String manager; // 추후 Resource Reference로 구현
}

/**
 * 정의되지 않은 커스텀 확장을 위한 Generic 클래스
 */
public class GenericExtension extends ExtensionData {
    private Map<String, Object> attributes = new HashMap<>();
    
    @JsonAnySetter
    public void add(String key, Object value) { attributes.put(key, value); }
    
    @JsonAnyGetter
    public Map<String, Object> getAttributes() { return attributes; }
}
```

## 3. 메시지 큐 페이로드 명세 (SCIM Aware)

### 3.1. 수집 이벤트 (HR -> Core)

```json
{
  "traceId": "trace-uuid",
  "payload": {
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User", "..."],
    "externalId": "H001",
    "userName": "hong.g@iam.com",
    "name": { "familyName": "Hong", "givenName": "Gildong", "formatted": "Hong Gildong" },
    "active": true,
    "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
      "employeeNumber": "H001",
      "department": "Dev Team"
    }
  }
}
```

## 4. 핵심 비즈니스 로직 (Core Service)

1. **Mapping:** 수신된 SCIM JSON을 `IamUser` (Flat)와 `IamUserExtension` (JSONB)으로 매핑합니다.
2. **Persistence:**
    * `IamUser`: `familyName`, `givenName`, `lastModified` 등 정형 컬럼 업데이트.
    * `IamUserExtension`: `extensions` JSONB 필드에 확장 데이터 병합.
3. **Audit:** 변경 이력을 `event_log`에 기록합니다.

## 5. MVP 개발 가이드 (Vibe Coding)

* **하이브리드 전략:** 검색과 필터링이 빈번한 Core 속성은 컬럼으로, 가변성이 높은 Extension은 JSONB로 관리합니다.
* **Type Safety:** 정형 컬럼을 통해 DB 레벨의 제약 조건을 활용합니다.
* **Lombok 활용:** `@Getter`, `@NoArgsConstructor` 사용.

### Security & Modernity Check

* **Why Java 21?**: 다형성 구조(`ExtensionData`)를 사용할 때 Java 21의 **Pattern Matching for switch**를 활용.
* **Security Note**: `userName`과 `externalId`에 유니크 제약 조건을 부여하고, PII(개인정보)인 성명 필드를 컬럼화하여 필요한 경우 DB 레벨의 암호화(TDE) 적용이 용이하도록 설계할 것.
* **Clean Architecture**: 엔티티 내부에서 비즈니스 로직을 직접 수행하지 않고, 도메인 모델로서의 상태 관리 역할에 충실할 것.
