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

* **속성 메타데이터 (IamAttributeMeta):** SCIM Schema Discovery 및 동적 변환 규칙의 기초가 되는 메타데이터.
  * **참조 파일:** `IamAttributeMeta.java`, `IamAttributeMetaDto.java`
  * **주요 필드:** `name` (SCIM 규격), `type` (데이터 타입), `multiValued`, `returned`, `uniqueness`, `mutability`.
  * **역활:** `/scim/v2/Schemas` 응답 생성 및 UI 컴포넌트(`uiComponent`) 렌더링 가이드 제공.

* **동적 리소스 저장소 (ScimDynamicResource):** 런타임에 정의된 새로운 리소스 타입을 위한 범용 저장소.
  * **참조 파일:** `ScimDynamicResource.java`, `ScimDynamicResourceService.java`
  * **ID 전략:**
    * **물리 키 (`id`)**: **TSID (Long)** 사용 (성능 및 정렬).
    * **논리 키 (`scimId`)**: URL 경로용 고유 식별자 (예: `laptop-001`, UUID).
  * **저장 방식:** PostgreSQL **JSONB**를 사용하여 임의의 속성 구조를 유연하게 수용.
  * **라우팅:** `ScimEndpointManager`를 통해 동적 리소스 타입 등록 시 `/scim/v2/{ResourceType}/**` 엔드포인트 자동 활성화.

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

## 5. 이력 관리 및 데이터 추적성 (Traceability)

IAM 시스템은 데이터의 변경 전후를 완벽하게 추적하고, 장애 발생 시 특정 시점의 상태를 복원할 수 있는 다중 이력 관리 체계를 갖추고 있습니다.

### 5.1 시스템 동기화 장부 (Sync History)

* **참조 파일:** `SyncHistory.java`, `SyncHistoryService.java`
* **추적 ID (traceId):** 모든 이벤트는 생성 시점부터 고유한 `traceId`를 공유하며, 이를 통해 `HR -> Core -> AD` 전체 흐름을 관통하여 추적합니다.
* **데이터 최적화:** * 기존의 단순 문자열 `payload`를 제거하고, **JSONB** 형식을 사용하는 `request_payload`와 `result_data`로 전환하였습니다.
  * **`request_payload`**: 유입된 `UserSyncEvent` 전문(Object)을 저장하여 당시의 원천 데이터 컨텍스트를 보존합니다.
  * **`result_data`**: 변경된 필드(`diff`) 및 처리 결과 메타데이터만 저장하여 스토리지 효율을 높였습니다.
* **시점 일관성 (Dual Revisions):**
  * **`user_rev_id`**: Hibernate Envers를 통해 기록된 당시 사용자 엔티티의 리비전 번호입니다.
  * **`rule_rev_id`**: 당시 데이터 변환에 적용되었던 Groovy 스크립트 및 매핑 설정의 리비전 번호입니다.
* **보관 정책:** `expires_at` 컬럼을 통해 30일(기본값) 후 자동 삭제되도록 관리합니다. (중복 데이터인 `duration_ms` 및 `completed_at`은 `created_at`으로 통합 관리하여 제거됨)

### 5.2 데이터 스냅샷 및 복원 (Audit Snapshot)

* **기술 스택:** Hibernate Envers (Audit Reader)
* **복원 메커니즘:**
  * **리비전 기반 복원:** `user_rev_id`를 사용하여 특정 시점의 `IamUser` 및 `Extension` 엔티티를 SCIM 2.0 규격으로 완벽하게 복원합니다.
  * **Trace ID 기반 복원:** `CustomRevisionEntity`를 통해 엔버스의 리비전에 `traceId`를 직접 심어, 장부의 ID만으로도 당시의 데이터 상태를 즉시 조회할 수 있습니다.
* **규칙 복원:** `rule_rev_id`와 `system_id`를 조합하여 동기화 당시에 실행되었던 Groovy 스크립트 내용과 컬럼 매핑 정보를 조회할 수 있습니다.

### 5.3 주요 API 명세 (이력 관련)

* **사용자 이력 스냅샷:** `GET /api/v1/history/users/{id}/trace_id/{traceId}`
* **컬럼 매핑 이력:** `GET /api/v1/rules/history?systemId={id}&revId={revId}`
* **동기화 목록 조회:** `GET /api/v1/history` (Pageable 지원 및 필터링 최적화)

## 6. 개발 가이드 (AI 참조용)

* **엔티티 수정 시:** `@Audited`가 선언된 엔티티 변경 시 반드시 `UserRevisionListener`를 통해 `traceId`가 리비전에 기록되도록 보장해야 합니다.
* **API 개발 시:** 조회 시 결과가 없을 경우 `NoResultException` 대신 `IAM-4103 (RESOURCE_NOT_FOUND)` 에러 코드를 사용하여 일관된 응답을 제공합니다.
* **Primary Key:** 모든 주요 테이블은 **TSID** (Time-Sorted Unique Identifier)를 사용합니다.
* **Validation:** `userName` 및 `externalId`에 유니크 제약 조건을 보장합니다.
* **Consistency:** 코드 수정 시 반드시 이 문서의 아키텍처 방향성을 유지하고, 구현 결과는 다시 이 문서의 '구현 현황' 섹션에 업데이트합니다.

## 7. 구현 현황 (Status)

### 7.1 데이터 모델 및 영속성 (Data Model)

* **엔티티 통합**: `IamUser` + `IamUserExtension` (OneToOne) 통합 관리 완료
* **감사 추적**: Hibernate Envers 기반 핵심 엔티티 리비전 기록 완료
* **커스텀 리비전**: `traceId`를 리비전에 직접 기록하는 `CustomRevisionEntity` 구현 완료

### 7.2 동기화 장부 및 이력 (Sync & History)

* **이력 최적화**: 중복 필드(`duration_ms`, `completed_at`) 제거 및 `created_at` 단일화 완료
* **페이로드 보존**: `UserSyncEvent` 전문을 JSONB(`request_payload`)로 저장 로직 완료
* **조회 최적화**: 조건별 동적 필터링 및 페이지네이션 기반 조회 API 구현 완료

### 7.3 스냅샷 복원 및 조회 (Snapshot Recovery)

* **Revision 조회**: 특정 `revId` 시점의 사용자 SCIM 프로필 복구 API 구현 완료
* **Trace ID 조회**: 비즈니스 `traceId` 기반 시점 데이터 복구 API 구현 완료
* **매핑 이력 조회**: 특정 시스템 및 시점의 컬럼 매핑 리스트 복원 API 구현 완료

### 7.4 연동 및 프로비저닝 (Integration)

* **AD 연동**: AD 전용 `ProvisioningCommand` 발행 및 이력 통합 기록 완료
* **AD 커넥터**: AD로부터의 응답(Result) 수신 및 `SyncHistory` 상태 업데이트 로직 구현 중

### 7.5 SCIM 표준화 및 메타데이터 (SCIM Alignment)

* **스키마 발견**: `/scim/v2/Schemas` 및 `/scim/v2/ResourceTypes` API 구현 완료
* **예외 표준화**: `IamAttributeMetaService` 내의 예외 처리를 `IamBusinessException` 표준으로 전환 완료

### 7.6 동적 리소스 지원 (Dynamic Resources)

* **범용 엔티티**: TSID와 JSONB를 결합한 `ScimDynamicResource` 모델링 완료
* **동적 라우팅**: `ScimEndpointManager`를 이용한 런타임 엔드포인트 등록 및 `{id}` 하위 경로 지원 완료
* **자동 CRUD**: `GenericScimController`를 통한 범용 리소스 생명주기 처리 구현 완료
