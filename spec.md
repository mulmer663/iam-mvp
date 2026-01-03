# 📜 IAM MVP 기술 명세서 (Version 1.4 - Reference Based)

## 1. MVP 목표: "HR 데이터 수집 및 SCIM 2.0 규격 변환 -> IAM 내부 사용자 생성(Core/Extension 분리) -> 식별자 매핑 -> 대상 시스템 프로비저닝"

## 2. 시스템 아키텍처 및 데이터 흐름

SCIM 2.0 프로토콜을 기반으로 외부 시스템(HR)으로부터 데이터를 수신하여 IAM Core에 저장하고, 대상 시스템(AD)으로 프로비저닝합니다.

* **iam-connector-hr (Source Event Detector):**  
  * HR 시스템의 변화(신규 입사, 정보 수정, 퇴사)를 감지합니다.
  * 복잡한 변환 없이 **원천 데이터 스냅샷**을 포함한 이벤트를 `iam.ingest` 큐로 던집니다.
  * **보상 트랜잭션 (Compensation):** Core 처리 실패 시 `iam.event.compensation` 이벤트를 수신하여 스냅샷을 **롤백(Revert)** 하거나(업데이트 실패 시), 삭제(신규 실패 시)하여 다음 배치 시 재처리를 보장합니다.
  * 이 시점에 **`traceId`**를 생성하여 전체 프로세스의 추적성을 시작합니다.

* **iam-core (Transformation & Orchestration):**  
  * 큐에서 이벤트를 수신하여 **Groovy Rule Engine**을 구동합니다.
  * 원천 데이터를 SCIM 표준 및 IAM 내부 스키마(`IamUser`, `JSONB`)로 변환합니다.
  * 변환 전/후 데이터를 `SyncHistory`에 기록하고 DB에 최종 반영합니다.
  * **실패 감지:** 동기화 실패 시 명시적으로 **`SyncCompensationEvent`**를 발행하여 원천 시스템에 롤백을 요청합니다.

* **Core Storage:** 정형 속성(Core)은 컬럼으로, 비정형 속성(Extensions)은 JSONB로 분리 저장하는 하이브리드 방식을 채택합니다.

```mermaid
graph LR
    subgraph "External"
        HR[HR System]
    end

    subgraph "IAM System"
        direction TB
        subgraph "Ingestion"
         HRC[HR Connector]
        end
        
        MQ1((iam.ingest))
        MQ_FAIL((iam.fail))
        
        subgraph "Core Domain (Rule Engine)"
            Logic[Sync Service]
            Rule[Groovy Engine]
            DB[(PostgreSQL)]
        end
    end

    HR -->|1. Detect Change| HRC
    HRC -->|2. Raw Event + traceId| MQ1
    MQ1 -->|3. Consume| Logic
    Logic <-->|4. Transform| Rule
    Logic <-->|5. Store & Audit| DB
    Logic -.->|6. On Fail (Compensation)| MQ_FAIL
    MQ_FAIL -.->|7. Revert Snapshot| HRC
```

## 3. 데이터 모델 전략 (Hybrid Storage)

상세 구현은 iam-core 모듈의 엔티티 클래스를 참조하십시오.

* **Core Attributes (정형):** 검색, 필터링, 권한 제어에 빈번하게 사용되는 속성.
  * **참조 파일:** `IamUser.java`
  * **주요 필드:** `userName`, `externalId`, `active`, `name` (Flattened).

* **Extension Attributes (비정형):** 가변성이 높고 시스템별로 상이한 확장 속성.
  * **참조 파일:** `IamUserExtension.java`, `ExtensionData.java`
  * **저장 방식:** PostgreSQL **JSONB**를 사용하여 스키마 드래프트 없이 유연하게 대응.
  * **다형성 처리:** `EnterpriseUserExtension.java` 등 URN 기반 다형성 매핑 적용.

* **식별자 매핑:** 외부 시스템과 IAM 간의 ID 매핑 정보를 관리합니다.

## 4. 데이터 연동 엔진 (Rule Engine)

런타임에 동적으로 데이터 변환 로직을 처리하며, 모든 이력은 추적 가능해야 합니다.

* **변환 패턴 및 규칙 매핑:**
  * **참조 파일:** `TransFieldMapping.java`, `RuleScriptGenerator.java`
  * **패턴 유형:**
    * `DIRECT`: 1:1 단순 필드 배핑.
    * `CODE`: DB(`IAM_TRANS_CODE_VALUE`) 또는 파라미터 기반 코드 매핑. 매핑 정보가 없는 경우 원본값을 유지하는 Pass-through 방식 지원.
    * `CLASSIFY` / `REPLACE`: 키워드 포함 여부 또는 값 일치 여부에 따른 변환.
    * `CUSTOM`: 자유도가 높은 커스텀 Groovy 스니펫 실행.
  * **내장 검증:** 생성된 스크립트 내에 필수성(`isRequired`), 최소/최대 길이 검증 로직을 자동 포함함.

* **예외 처리 및 견고성:**
  * **정교한 예외 분류:** `RuleCompilationException`(문법), `RuleValidationException`(검증), `RuleExecutionException`(런타임) 등 상황별 전용 예외 처리.
  * **샌드박스 보안:** `SecureASTCustomizer`를 통한 화이트리스트 기반 샌드박스 실행.

* **이력 관리 (Traceability):**
  * **참조 파일:** [SyncHistory.java](file:///c:/Dev/project/iam/iam-core/src/main/java/com/iam/core/domain/entity/SyncHistory.java)
  * **추적성 강화:** 모든 이벤트는 `traceId`를 공유하며, `parent_history_id`를 통한 단계별 부모-자식 관계를 형성하여 `HR -> Core -> AD` 흐름을 추적함.
  * **메타데이터:** `duration_ms`를 통해 각 변환/반영 단계별 처리 속도를 측정하고, `expires_at`을 통해 이력 보관 주기를 관리함.
  * **데이터 평탄화:** `UniversalData` 구조를 자동으로 언래핑하여 JSON 페이로드 가독성을 확보함.

* **데이터베이스:** `IAM_TRANS_RULE_META`, `IAM_TRANS_RULE_VERSION` 등 규칙 버전 관리 테이블 참조 (`schema.sql`).

## 5. 개발 가이드 (AI 참조용)

* **Primary Key:** 모든 주요 테이블은 **TSID** (Time-Sorted Unique Identifier)를 사용합니다.
* **Validation:** `userName` 및 `externalId`에 유니크 제약 조건을 보장합니다.
* **Consistency:** 코드 수정 시 반드시 이 문서의 아키텍처 방향성을 유지하고, 구현 결과는 다시 이 문서의 '구현 현황' 섹션에 업데이트합니다.

## 6. 구현 현황 (Status)

[x] Core/Extension 하이브리드 스토리지 설계
[x] TSID 기반 엔티티 구조 설계
[x] Groovy Rule Engine 샌드박스 및 동적 변환 엔진 구현
[x] HR Connector 스냅샷 가데이터 및 변경 이벤트 인제스트 로직
[x] 계층적 동기화 이력 관리 (Traceability) 및 페이로드 평탄화 구현
[x] 규칙 기반 동적 스크립트 엔진 (DIRECT/CODE/CLASSIFY/REPLACE/CUSTOM) 구현
[x] 변환 규칙 검증 필터(필수값/길이) 및 상황별 예외 처리 고도화
[x] 규칙 매핑 CRUD API 및 DB 기반 코드 매핑 연동 구현
[x] 보상 트랜잭션(Sync Compensation) 및 스냅샷 롤백 메커니즘 구현 - NEW
