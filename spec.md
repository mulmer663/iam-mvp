# IAM MVP 기술 명세서 (Version 3.0 — SCIM-First MSA)

> **최종 업데이트:** 2026-04-24  
> **현재 단계:** Pilot — SCIM 2.0 표준 스펙 충실 구현

---

## 1. MVP 목표

> **"HR 데이터 수집 → SCIM 2.0 규격 변환 → IAM Registry 저장(Golden Record) → 대상 시스템 프로비저닝"**

SCIM 2.0 (RFC 7643/7644) 표준을 충실히 구현하여 고객사별 다양한 속성을 타입 코드 변경 없이 DB 등록만으로 수용하는 확장형 Identity Fabric을 구축합니다.

---

## 2. 시스템 아키텍처

### 2.1 활성 마이크로서비스

| 모듈 | 포트 | 역할 |
|---|---|---|
| `iam-eureka` | 8761 | Service Discovery (Spring Cloud Eureka) |
| `iam-registry` | 18081 | Golden Record 저장소 + SCIM 2.0 REST API |
| `iam-engine` | 18082 | Groovy 4.x 기반 속성 변환 엔진 |
| `iam-adapter-db` | 18083 | 외부 DB 연결 어댑터 (HR 읽기 / AD 쓰기) |
| `iam-ui` | 5173 (dev) | Vue 3 관리 콘솔 (Miller Columns UI) |

> `iam-core` 는 **2026-04-22 세션에서 정식 퇴역**. `settings.gradle` 및 `docker-compose.yml` 에서 제거됨.

### 2.2 데이터 흐름 (Event Choreography)

```
HR DB → [iam-adapter-db] → RAW_INBOUND_DATA (RabbitMQ)
                               ↓
                        [iam-engine] (Groovy 변환)
                               ↓ CDM_DATA_QUEUE
                        [iam-registry] (Golden Record 저장)
                               ↓ (Change Event)
                        [iam-engine] → TARGET_OUTBOUND_DATA
                               ↓
                        [iam-adapter-db] → AD DB
```

---

## 3. iam-registry 상세 명세

### 3.1 데이터 모델

#### IamUser (Golden Record)

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long (IDENTITY) | 내부 PK |
| `externalId` | String | 외부 시스템 식별자 |
| `userName` | String | SCIM userName (unique) |
| `familyName / givenName / formattedName` | String | name 복합 속성 플래튼 |
| `title / active` | String / boolean | 코어 속성 |
| `emails / phoneNumbers` | `@OneToMany ScimMultiValue` | 다중값 속성 |
| `addresses` | `@OneToMany ScimAddress` | 주소 속성 |
| `extension` | `@OneToOne IamUserExtension` | 확장 속성 컨테이너 |
| `version` | Long (`@Version`) | Optimistic Lock |
| `created / lastModified` | LocalDateTime | 메타 타임스탬프 |

#### IamUserExtension (JSONB 확장 컨테이너)

```
schemas : List<String>           -- JSONB
extensions : Map<String, ExtensionData>  -- JSONB
```

- `extensions` 맵의 키는 SCIM 확장 URN (예: `urn:ietf:params:scim:schemas:extension:enterprise:2.0:User`)
- 특수 키 `__generic__` 는 메타데이터에 없는 알 수 없는 속성을 임시 수용
- **`ExtensionData`** 는 단일 구체 클래스 (`@JsonAnyGetter` / `@JsonAnySetter`) — 타입별 Java 클래스 없음

#### IamAttributeMeta (속성 메타데이터)

복합 PK `(name, targetDomain)` — `@IdClass(IamAttributeMetaId.class)`

| 필드 | 타입 | 설명 |
|---|---|---|
| `name` | String (PK) | 속성 리프 이름 |
| `targetDomain` | `AttributeTargetDomain` (PK) | `USER` / `GROUP` |
| `scimSchemaUri` | String | 소속 확장 URN (CORE이면 null) |
| `category` | `AttributeCategory` | `CORE` / `EXTENSION` / `CUSTOM` |
| `type` | `AttributeType` | `STRING` / `BOOLEAN` / `INTEGER` / `DECIMAL` / `DATETIME` / `REFERENCE` / `COMPLEX` |
| `multiValued` | boolean | 다중값 여부 |
| `required` | boolean | 필수 여부 |
| `caseExact` | boolean | RFC 7643 §2.2 |
| `mutability` | `AttributeMutability` | `READ_ONLY` / `READ_WRITE` / `IMMUTABLE` / `WRITE_ONLY` |
| `returned` | `AttributeReturned` | `ALWAYS` / `NEVER` / `DEFAULT` / `REQUEST` |
| `uniqueness` | `AttributeUniqueness` | `NONE` / `SERVER` / `GLOBAL` |
| `canonicalValues` | `List<String>` (JSONB) | RFC 7643 §7 표준 열거값 |
| `referenceTypes` | `List<String>` (JSONB) | `$ref` 참조 유형 힌트 |
| `description` | String | 속성 설명 |

**시드 데이터:** 애플리케이션 기동 시 `ScimMetaInitializer` 가 RFC 7643 표준 속성 **76개**를 자동 등록.  
고객 확장은 POST `/api/schemas` + POST `/api/attributes` 만으로 즉시 반영됨 — 코드 변경 불필요.

### 3.2 SCIM 2.0 API

**Base URL:** `http://localhost:18081`

#### SCIM User CRUD

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/scim/v2/Users` | 전체 사용자 목록 (ListResponse) |
| `GET` | `/scim/v2/Users/{id}` | 단일 사용자 조회 |
| `POST` | `/scim/v2/Users` | 사용자 생성 (201) |
| `PUT` | `/scim/v2/Users/{id}` | 사용자 전체 교체 |
| `DELETE` | `/scim/v2/Users/{id}` | 사용자 삭제 (204) |

**응답 형식:** `ScimUserResponse` — 확장 URN은 `@JsonAnyGetter` 로 최상위 키로 직렬화 (RFC 7643 §3.5 준수)

```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User",
               "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"],
  "id": "42",
  "userName": "jsmith",
  "name": { "familyName": "Smith", "givenName": "John" },
  "emails": [{ "value": "jsmith@example.com", "type": "work", "primary": true }],
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
    "employeeNumber": "E-1234",
    "department": "Engineering"
  },
  "meta": { "resourceType": "User", "location": "/scim/v2/Users/42" }
}
```

#### SCIM Discovery

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/scim/v2/Schemas` | 등록된 스키마 목록 |
| `GET` | `/scim/v2/Schemas/{uri}` | 특정 스키마 상세 |
| `GET` | `/scim/v2/ResourceTypes` | 지원 리소스 타입 목록 |
| `GET` | `/scim/v2/ResourceTypes/{name}` | 특정 리소스 타입 상세 |
| `GET` | `/scim/v2/ServiceProviderConfig` | 서비스 프로바이더 기능 선언 |

#### Admin API (속성/스키마 관리)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/attributes?domain=USER` | 도메인별 속성 목록 |
| `POST` | `/api/attributes` | 속성 메타 등록 |
| `PUT` | `/api/attributes/{domain}/{name}` | 속성 메타 수정 |
| `DELETE` | `/api/attributes/{domain}/{name}` | 속성 메타 삭제 (CORE 제외) |
| `GET` | `/api/schemas` | 스키마 목록 |
| `POST` | `/api/schemas` | 커스텀 스키마 등록 |

#### 동적 SCIM API (실험)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/scim/v2/{ResourceType}` | 동적 리소스 목록 |
| `POST` | `/scim/v2/{ResourceType}` | 동적 리소스 생성 |
| `PUT` | `/scim/v2/{ResourceType}/{id}` | 동적 리소스 수정 |
| `DELETE` | `/scim/v2/{ResourceType}/{id}` | 동적 리소스 삭제 |

### 3.3 공통 에러 응답

```json
{
  "errorCode": "IAM-4103",
  "message": "User not found: 99",
  "traceId": "...",
  "timestamp": "2026-04-24T10:00:00",
  "path": "/scim/v2/Users/99",
  "status": 404
}
```

---

## 4. 고객사 확장 속성 등록 워크플로우

코드 변경 없이 DB 조작만으로 신규 확장 속성을 즉시 서비스에 반영합니다.

```
1. POST /api/schemas
   { "id": "urn:acme:scim:schemas:extension:1.0:User",
     "name": "AcmeUser", "resourceType": "User", ... }

2. POST /api/attributes  (반복 — 속성 개수만큼)
   { "name": "costCenter", "targetDomain": "USER",
     "scimSchemaUri": "urn:acme:scim:schemas:extension:1.0:User",
     "category": "EXTENSION", "type": "STRING" }

3. GET /scim/v2/Schemas  →  신규 스키마 즉시 노출
4. POST /scim/v2/Users 에 URN 키로 확장 속성 전송 가능
```

---

## 5. 이력 관리 (Traceability)

| 메커니즘 | 구현 | 설명 |
|---|---|---|
| Hibernate Envers | `@Audited` on IamUser, IamUserExtension | 엔티티 전체 스냅샷 |
| CustomRevisionEntity | `traceId` 필드 추가 | 변환 파이프라인 연결 |
| SyncHistory | `request_payload` (JSONB) + `result_data` (JSONB) | 동기화 이벤트 장부 |

---

## 6. 데이터 변환 엔진 (iam-engine)

| 변환 패턴 | 설명 |
|---|---|
| `DIRECT` | 1:1 단순 필드 매핑 |
| `CODE` | 내부 코드 테이블 치환 (미정의 시 Pass-through) |
| `CLASSIFY` / `REPLACE` | 키워드 기반 조작 |
| `CUSTOM` | Groovy snippet 자유 실행 (SecureASTCustomizer 샌드박스) |

---

## 7. 기술 스택

| 항목 | 선택 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.3 |
| Build | Gradle 8 (multi-module) |
| DB | PostgreSQL 16 (JSONB) |
| ORM | Hibernate 6 + Spring Data JPA |
| Audit | Hibernate Envers |
| Messaging | RabbitMQ 3.x |
| Service Discovery | Spring Cloud Eureka |
| Rule Engine | Groovy 4.x (SecureASTCustomizer) |
| Frontend | Vue 3 + TypeScript + Vite + Tailwind CSS |
| Package Manager | pnpm |
| Container | Docker Compose |

---

## 8. DB 정책 (Pilot)

- `spring.jpa.hibernate.ddl-auto: create-drop` — 파일럿 기간 동안 기동 시 스키마 재생성
- 운영 전환 시 Flyway 마이그레이션으로 교체 예정

---

## 9. 관련 문서

- `CLAUDE.md` — Claude Code 컨텍스트 및 커밋 가이드
- `iam-registry/CLAUDE.md` — 레지스트리 모듈 편집 규칙
- `iam-ui/CLAUDE.md` — 프론트엔드 편집 규칙
- `iam-core/CLAUDE.md` — **RETIRED** (참조 불가)
