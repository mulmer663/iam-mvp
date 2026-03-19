# IAM 데이터 통합 엔진 설계 요약

## 기술 스택 및 엔진 전략 (Hybrid Engine)

- **기반:** Java Spring Boot 환경.
- **동적 변환:** 단순 매핑을 넘어선 복잡한 로직 처리를 위해 **Groovy 스크립트 엔진**을 채택.
- **사용자 경험:** 관리자는 UI에서 규칙을 설정하고, 시스템은 이를 내부적으로 Groovy 코드로 변환하여 실행하는 **Low-Code** 지향.

## 데이터 추상화 (Universal Mapping)

- **표준화:** AD, DB, API 등 다양한 원천 데이터를 `Map<String, Data>` 형태의 **Key-Value 구조**로 추상화.
- **단방향성:** 원천(Source)에서 IAM으로의 데이터 흐름에 집중하여 정합성 관리의 복잡도를 낮춤.
- **Data 인터페이스**: Sealed Interface + 구현체 (String, Time, Integer 형태의 저장소 클래스)

## 보안 및 거버넌스 (Security & Control)

- **샌드박스:** `SecureASTCustomizer`를 통해 허용된 클래스와 메서드만 실행 가능한 **화이트리스트 기반 보안** 적용.
- **버전 관리:** DB 내에 스크립트 본문과 해시값을 저장하여 **불변(Immutable) 히스토리**와 **추적성(Audit Trail)** 확보.

## 성능 및 운영 최적화 (Efficiency)

- **캐싱:** 스크립트 해시를 활용하여 컴파일된 클래스를 재사용함으로써 대량 데이터 처리 성능 극대화.
- **안정성:** 운영 반영 전 영향도를 분석하는 **Shadow Execution** 및 **Dry-Run** 개념 도입.

## 에러 유형의 상세 분류 및 대응 전략

|**에러 레벨**|**유형**|**주요 원인**|**재처리 여부**|**관리자 조치 사항**|
|---|---|---|---|---|
|**L1: 변환 엔진**|**스크립트 오류**|그루비 문법 오류, Null 참조, 0 나누기 등|**No**|스크립트 로직 수정 및 테스트|
|**L2: 데이터 검증**|**제약 조건 위반**|DB 길이 초과, 형식 불일치(Regex 실패), 필수값 누락|**No**|원천 데이터 수정 또는 변환 규칙 완화|
|**L3: 인프라/런타임**|**일시적 실패**|DB Lock 경합, 네트워크 타임아웃, 커넥션 풀 부족|**Yes (자동)**|시스템 모니터링 (반복 시 인프라 점검)|

## '전략적 원자성'의 수립

- **핵심 속성(Core):** 하나라도 실패 시 레코드 전체 롤백 $\to$ **데이터 무결성 우선** (UPDATE_CRITICAL)
- **일반 속성(Normal):** 실패 시 해당 속성만 이전 값 유지, 나머지는 반영 $\to$ **가용성 우선**

#### SecureASTCustomizer

```java
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.control.customizers.*
import org.codehaus.groovy.control.CompilerConfiguration

public CompilerConfiguration getSecureConfig() {
    SecureASTCustomizer customizer = new SecureASTCustomizer();

    // 1. 기본 표현식 제어 (연산자 등 허용)
    customizer.setClosuresAllowed(false); // 클로저 차단 (무한 루프 방지)
    customizer.setMethodDefinitionAllowed(false); // 내부 메서드 정의 차단

    // 2. 패키지 및 클래스 화이트리스트 설정
    List<String> whiteList = Arrays.asList(
        "java.lang.String",
        "java.lang.Integer",
        "java.lang.Double",
        "java.lang.Boolean",
        "java.util.Map",
        "java.util.List",
        "java.util.ArrayList",
        "java.util.HashMap",
        "java.util.Date",
        "java.text.SimpleDateFormat"
    );
    customizer.setImportWhitelist(whiteList);
    customizer.setReceiversClassesWhiteList(whiteList);

    // 3. 차단할 키워드 (강력 제어)
    customizer.setStaticStarImportBlacklist(Arrays.asList("java.lang.System", "java.lang.Runtime"));
    
    // 4. 컴파일 구성에 추가
    CompilerConfiguration config = new CompilerConfiguration();
    config.addCompilationCustomizers(customizer);
    
    return config;
}
```

## DDL

```sql
/* 원천 시스템의 유형과 접속 정보를 관리합니다. */
CREATE TABLE IAM_SOURCE_SYSTEM (
    SYSTEM_ID           VARCHAR(50)  PRIMARY KEY,    -- 시스템 식별자 (예: AD_HQ, DB_HR)
    SYSTEM_NAME         VARCHAR(100) NOT NULL,       -- 시스템 명칭
    SYSTEM_TYPE         VARCHAR(20)  NOT NULL,       -- AD, JDBC, REST, CSV 등
    CONN_INFO           JSONB        NOT NULL,       -- 접속 정보 (암호화하여 저장 추천)
    IS_ACTIVE           BOOLEAN      DEFAULT TRUE,   -- 활성화 여부
    CREATED_AT          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY          VARCHAR(50),
    UPDATED_AT          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE IAM_SOURCE_SYSTEM IS '원천 HR 시스템 정보 관리';


/* 변환 규칙의 정의와 목적을 관리합니다. */
CREATE TABLE IAM_TRANS_RULE_META (
    RULE_ID             VARCHAR(50)  PRIMARY KEY,    -- 규칙 식별자
    RULE_NAME           VARCHAR(100) NOT NULL,       -- 규칙 이름 (예: 사번추출, 부서매핑)
    TARGET_ATTR         VARCHAR(50)  NOT NULL,       -- IAM 측 대상 속성명
    DESCRIPTION         TEXT,                        -- 규칙 설명
    STATUS              VARCHAR(20)  DEFAULT 'DRAFT',-- DRAFT, ACTIVE, RETIRED
    CREATED_AT          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE IAM_TRANS_RULE_META IS '데이터 변환 규칙 메타데이터';


/* 실제 Groovy 스크립트 본문과 버전을 관리합니다. **불변성** 유지를 위해 수정 시 새로운 레코드를 생성합니다. */
CREATE TABLE IAM_TRANS_RULE_VERSION (
    VER_ID              SERIAL       PRIMARY KEY,    -- 버전 고유 ID
    RULE_ID             VARCHAR(50)  NOT NULL,       -- 메타 참조
    VERSION_NO          INTEGER      NOT NULL,       -- 버전 번호 (1, 2, 3...)
    SCRIPT_CONTENT      TEXT         NOT NULL,       -- Groovy 스크립트 본문
    SCRIPT_HASH         VARCHAR(64)  NOT NULL,       -- 무결성 및 중복 컴파일 방지용 해시
    CHANGE_LOG          TEXT,                        -- 변경 사유
    IS_CURRENT          BOOLEAN      DEFAULT FALSE,  -- 현재 운영 적용 버전 여부
    CREATED_AT          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY          VARCHAR(50),
    CONSTRAINT FK_RULE_META FOREIGN KEY (RULE_ID) REFERENCES IAM_TRANS_RULE_META(RULE_ID)
);

CREATE INDEX IDX_RULE_VER_CURRENT ON IAM_TRANS_RULE_VERSION(RULE_ID, IS_CURRENT);
COMMENT ON TABLE IAM_TRANS_RULE_VERSION IS '변환 규칙 스크립트 버전 관리';



/* 어떤 시스템에 어떤 규칙을 어떤 순서로 적용할지 정의합니다. */
CREATE TABLE IAM_TRANS_MAPPING (
    MAP_ID              SERIAL       PRIMARY KEY,
    SYSTEM_ID           VARCHAR(50)  NOT NULL,
    RULE_ID             VARCHAR(50)  NOT NULL,
    EXEC_ORDER          INTEGER      DEFAULT 0,      -- 실행 순서 (낮을수록 먼저 실행)
    IS_MANDATORY        BOOLEAN      DEFAULT TRUE,   -- 필수 성공 여부
    CREATED_AT          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_MAP_SYSTEM FOREIGN KEY (SYSTEM_ID) REFERENCES IAM_SOURCE_SYSTEM(SYSTEM_ID),
    CONSTRAINT FK_MAP_RULE FOREIGN KEY (RULE_ID) REFERENCES IAM_TRANS_RULE_META(RULE_ID)
);

COMMENT ON TABLE IAM_TRANS_MAPPING IS '원천 시스템별 변환 규칙 매핑';

/* 데이터 변환 과정에서 발생하는 정교한 오류를 캡처하기 위한 테이블입니다. `SyncHistory`와 1:N 관계를 가집니다. */
CREATE TABLE IAM_sync_transform_failure (
    failure_id          BIGINT PRIMARY KEY,     -- TSID 사용 권장
    history_id          BIGINT NOT NULL,        -- sync_history.id 외래키
    field_name          VARCHAR(100),           -- 실패한 필드명 (예: "email", "employeeId")
    invalid_value       TEXT,                   -- 문제가 된 실제 값
    rule_name           VARCHAR(100),           -- 위반한 규칙명 (예: "EmailFormatValidator")
    error_type          VARCHAR(50),            -- 오류 유형 (MISSING, INVALID_FORMAT, DUPLICATE)
    error_message       TEXT,                   -- 상세 에러 메시지
    suggested_fix       TEXT,                   -- 시스템 추천 수정사항
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_sync_history FOREIGN KEY (history_id) 
        REFERENCES sync_history(history_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_transform_history ON sync_transform_failure(history_id);
CREATE INDEX idx_transform_field ON sync_transform_failure(field_name);
```

#### 수정된 SyncHistory

```java
@Entity
@Table(name = "sync_history", indexes = {
    @Index(name = "idx_sync_trace", columnList = "traceId"),
    @Index(name = "idx_sync_target", columnList = "targetUser")
})
@Getter @Setter @NoArgsConstructor
public class SyncHistory {
    @Id @Tsid
    private Long id;

    @Column(length = 64, nullable = false)
    private String traceId;

    @Column(length = 32, nullable = false)
    private String type; // HR_SYNC, USER_UPDATE, AD_PROVISION

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SyncStatus status; // PENDING, SUCCESS, FAILURE, PARTIAL_SUCCESS

    @Column(length = 100)
    private String sourceSystem; // 예: "SAP_HR", "WORKDAY"

    @Column(length = 100)
    private String targetSystem; // 예: "AZURE_AD", "LOCAL_LDAP"

    private String targetUser;

    @Column(columnDefinition = "LONGTEXT")
    private String requestPayload; // 변환 전 원본 데이터

    @Column(columnDefinition = "LONGTEXT")
    private String responsePayload; // 결과 데이터 또는 반환값

    private Integer retryCount = 0;

    private Long parentHistoryId; // 재시도 시 이전 이력 참조

    private Long durationMs; // 소요 시간 (ms)

    private LocalDateTime createdAt;
    
    private LocalDateTime completedAt;

    private LocalDateTime expiresAt; // 데이터 보존 기한
}
```
