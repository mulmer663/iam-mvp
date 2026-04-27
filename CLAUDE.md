# IAM MVP — Claude Code 작업 가이드

> 이 문서는 Claude Code 가 자동 로드하는 루트 컨텍스트입니다.
> 도메인 상세는 [spec.md](./spec.md), 거버넌스/AI 에이전트 규칙은 [AGENTS.md](./AGENTS.md), 모듈 상세는 각 모듈의 `CLAUDE.md` / `AGENTS.md` 를 참조하세요.

## 프로젝트 한 줄 요약

**HR → IAM Registry → Target System** 데이터 파이프라인을 RabbitMQ 이벤트 choreography 로 분리하고, 변환 규칙을 Groovy 런타임 엔진에서 실행하는 **SCIM 2.0 기반 MSA IAM MVP**.

- Antigravity 로 개발 중 중단된 토이 프로젝트 (개인 실험)
- 상세 배경: [README.md](./README.md)

## 🎯 현재 집중 영역 (Now)

**SCIM 2.0 스키마 확장(Extension) 및 속성 메타 상세 스펙화**.

표준 `User` / `Group` 리소스의 고정 속성만으로는 고객사별 다양한 속성 요구(사번, 직급 체계, 커스텀 역할, 국가별 필드, 내부 분류 코드 등)를 커버할 수 없기 때문에, **스키마를 런타임 확장 가능하게 만드는 것**이 우선 과제입니다.

### 이미 구축된 확장 인프라 (`iam-registry/domain/scim/`)

| 엔티티 | 역할 |
|---|---|
| `ScimSchemaMeta` | 스키마 URN 레지스트리 (`urn:ietf:params:scim:schemas:core:2.0:User` 등). 속성 리스트 보유 |
| `IamAttributeMeta` | 속성 단위 메타 — RFC 7643 특성(`mutability`/`returned`/`uniqueness`/`multiValued`/`required`) + 프로젝트 확장(`targetDomain`, `category`, `adminOnly`, `view/editLevel`, `encrypted`, `uiComponent`, `display`) |
| `ScimResourceTypeMeta` | ResourceType 정의 (id/endpoint/main schema + 확장 스키마 목록) |
| `ScimResourceTypeExtension` | ResourceType ↔ Extension 스키마 (required 여부) 임베디드 연결 |
| `ScimDynamicResource` | 런타임 등록된 리소스 타입의 실제 저장 (JSONB `attributes`) |
| `IamUserExtension` / `ExtensionData` | User 확장 속성 JSONB 저장 — Map<URN, ExtensionData> 형태로 임의 키/값 보관 (URN 별 typed 클래스 없음) |

### 이 영역에서 편집 시 우선 체크할 것

1. **RFC 7643 Section 2 준수** — `IamAttributeMeta` 의 핵심 필드 (`caseExact` / `canonicalValues` / `referenceTypes` / `display`) 는 추가 완료. 남은 갭:
   - 시드된 sub-attribute 의 `canonicalValues` 가 비어 있음 (예: `emails.type` → 표준은 `["work","home","other","unknown"]`)
   - `subAttributes` 관계가 `parentName` 문자열 기반 — 멀티 레벨 중첩 대응은 미검증
2. **`/Schemas`, `/ResourceTypes`, `/ServiceProviderConfig` 디스커버리 응답**이 `IamAttributeMeta` / `ScimSchemaMeta` / `ScimResourceTypeMeta` 에서 **정확히 재구성**되는지
3. **확장 속성 PATCH** 경로 (`"schemas"` 배열에 확장 URN 포함, 값 타겟팅 `urn:...:User:department` 형태)
4. **동적 ResourceType CRUD** (`ScimDynamicResource`) 가 표준 `/scim/v2/{ResourceType}` 규약을 그대로 따르는지
5. 고객사 확장 추가 시 **코드 변경 없이 DB 만으로** — `POST /api/schemas` + `POST /api/attributes` 만으로 `/scim/v2/Schemas` 에 즉시 반영됨이 검증 완료 (한국 가상 확장 E2E)

- 레퍼런스 스펙: [`SCIM.txt`](./SCIM.txt) (RFC 7643/7644 원문 사본)
- 내부 계약: [`spec.md`](./spec.md) §6.6, §6.7 — 표준과 충돌 시 **표준 우선** 후 spec.md 갱신

### 최종 목표 (Eventually, 지금은 보류)

궁극적 아키텍처 비전: [`archive/iam-architecture.docx`](./archive/iam-architecture.docx) — Universal Identity Fabric & Transformation Engine.
현재는 이 그림을 그대로 만들지 않고 **스키마 확장 기반이 안정화된 뒤** 재검토. 새 작업 제안 시 *"이게 스키마 확장 인프라를 고도화하는가, 아니면 벗어난 방향인가?"* 를 먼저 확인.

## 활성 모듈 맵

| 모듈 | 역할 | 포트(Docker) |
|---|---|---|
| `iam-eureka` | Service Discovery (Spring Cloud Netflix) | 8761 |
| `iam-registry` | SCIM 2.0 Identity Registry (Golden Record, JPA + Envers) | 18081 |
| `iam-engine` | Groovy 런타임 변환 엔진 (stateless, sandboxed) | 18085→8080 |
| `iam-adapter-db` | DB Connectivity Adapter (RabbitMQ in/out, 순수 JDBC) | 18086→8080 |
| `iam-ui` | Vue 3 + TS 프론트엔드 (Miller Columns UX) | 15173→80 |

> 레거시 `iam-core` 모듈은 `settings.gradle` / `docker-compose.yml` 에서 제거되어 빌드/실행 라인에서 빠졌습니다 (소스 디렉터리는 참고용으로 보존). `iam-registry` 가 모든 SCIM 메타/스키마 책임을 가집니다.

## 기술 스택 (검증 완료)

- **Language/Build:** Java 21, Gradle (멀티모듈), `settings.gradle` 에 등록된 모듈이 실제 소스
- **Framework:** Spring Boot **3.4.3**, Spring Cloud 2024.0.0
- **Messaging:** RabbitMQ (Event Choreography)
- **Persistence:** PostgreSQL 17 + Hibernate JPA, **Hibernate Envers** (traceId 스냅샷)
- **Storage 전략:** Core 속성 = 정형 컬럼 / Extension 속성 = PostgreSQL **JSONB**
- **Rule Engine:** Groovy 4.x + `SecureASTCustomizer` 샌드박스
- **ID 전략:** Hypersistence **TSID (Long)** 또는 UUID (SCIM 호환)
- **Frontend:** Vue 3 (Composition API) + Vite + TypeScript + Tailwind + Shadcn Vue + Pinia, 패키지 매니저 `pnpm`

## 자주 쓰는 명령어

### 백엔드 (Gradle)

```bash
./gradlew build                       # 전체 빌드
./gradlew test                        # 전체 테스트
./gradlew :iam-registry:bootRun       # 개별 모듈 실행
./gradlew :iam-registry:test          # 개별 모듈 테스트
```

### 프론트엔드 (pnpm)

```bash
cd iam-ui
pnpm install
pnpm run dev                          # 개발 서버 (Vite)
pnpm run build                        # 빌드 (vue-tsc 타입 체크 포함)
```

### 실행 방식 (택 1)

| 시나리오 | 명령 |
|---|---|
| **하이브리드 (권장)** — 인프라만 Docker, 앱은 IDE | `docker-compose -f docker-compose.infra.yml up -d` |
| **풀 컨테이너** | `docker-compose up -d` (모든 활성 모듈 + Postgres + RabbitMQ) |

하이브리드 모드에서 앱은 `localhost:5432` (Postgres), `localhost:5672` (RabbitMQ), `localhost:8761` (Eureka) 에 자동 연결되도록 설정되어 있음.

## 데이터 흐름 (요약)

```
HR DB ──> iam-adapter-db ──[RAW_INBOUND_DATA]──> iam-engine
                                                      │
                                                      ▼ Groovy 변환
                                              [CDM_DATA_QUEUE]
                                                      │
                                                      ▼
                                               iam-registry ──> PostgreSQL
                                                      │
                       ┌──────── Change Event ────────┘
                       ▼
                  iam-engine ──[TARGET_OUTBOUND_DATA]──> iam-adapter-db ──> Target DB (AD 등)
```

전체 다이어그램과 큐 명세는 [spec.md](./spec.md) 섹션 2 참조.

## 반드시 지킬 규칙 (Hard Rules)

### Java / 백엔드

- `@Autowired` **필드 주입 금지** → Lombok `@RequiredArgsConstructor` + `final` 필드 사용
- `System.out.println` **금지** → SLF4J 로거 사용
- 시크릿/DB 패스워드 **하드코딩 금지** → 환경 변수
- 속성명/이벤트 타입/상태 코드는 **`AttributeConstants` / `ScimConstants`** 등 상수 사용
- `iam-registry` 는 어댑터 모듈에 **의존 금지** (단방향 이벤트만)
- 엔티티 직접 노출 금지 → DTO 사용
- Groovy 스크립트 실행 시 **`SecureASTCustomizer` 필수**, `System` / `Runtime` / `ProcessBuilder` 화이트리스트 금지
- 모든 `IamUser` / `SyncHistory` 변경은 **`traceId` 유지** (Envers `CustomRevisionEntity` 경유)

### Vue / 프론트엔드

- `<script setup lang="ts">` **필수**, Options API 금지
- 아이콘은 `lucide-vue-next`, 시스템 테마는 `@/utils/theme.ts` 의 `SYSTEM_THEMES` 만 사용
- 페이지 라우팅 대신 **Miller Columns** (`useMillerStore` 의 `MillerPane` 스택)
- 화면 검증 전 **반드시 `pnpm run build` 통과 확인**
- 밀도 우선 디자인: radius ≤ 4px, base font 13px, Neutral 팔레트 (색은 상태 표시 전용)

### Git / 형상관리 (조직 규칙)

- **신규/버그 작업은 브랜치 생성 후 병합 필수** — main 직접 커밋 금지
- Semantic 커밋 메시지 (`feat:`, `fix:`, `chore:`, `docs:` …)

## 현재 구현 상태 (README 기준)

- [x] MSA 모듈 분리 / 서비스 디스커버리
- [x] RabbitMQ 이벤트 파이프라인
- [x] Groovy 런타임 룰 엔진 + 샌드박싱
- [x] SCIM 2.0 표준 API (Users / Schemas / ResourceTypes)
- [x] Core / Extension 하이브리드 스토리지
- [x] Hibernate Envers 기반 이력 추적
- [x] `/scim/v2/Users` CRUD (ScimUserController — POST/GET/PUT/PATCH/DELETE)
- [x] UI 사용자 등록/수정 — `DynamicUserForm` (속성 메타 기반 동적 렌더링, `display` 플래그 반영)
- [x] `IamAttributeMeta.display` 플래그 — UI 등록/수정 화면 노출 여부 제어
- [ ] 프로비저닝 **아웃바운드** 파이프라인 (진행 중)
- [ ] UI ↔ 백엔드 연동 완성 (사용자 목록·상세 조회, 부서 트리 실데이터 연결)

작업 재개 시점 파악은 `git log --oneline` + 이 체크리스트 교차 확인.

## 문서 네비게이션

| 찾는 것 | 파일 |
|---|---|
| 도메인 / API / 데이터 모델 상세 | [spec.md](./spec.md) |
| 프로젝트 철학 / 왜 만들었나 | [README.md](./README.md) |
| AI 에이전트 거버넌스 (Antigravity 호환) | [AGENTS.md](./AGENTS.md) |
| Java 코드 스타일 | [.agent/rules/java-code-style-guide.md](./.agent/rules/java-code-style-guide.md) |
| Vue 코드 스타일 | [.agent/rules/vue-code-style-guide.md](./.agent/rules/vue-code-style-guide.md) |
| 하이브리드 개발 워크플로우 | [.agent/workflows/hybrid-dev.md](./.agent/workflows/hybrid-dev.md) |
| 레거시 설계 문서 | [archive/](./archive/) |

모듈별 상세 규칙은 각 모듈 폴더의 `CLAUDE.md` → `AGENTS.md` 참조 체인을 따름.

## 알려진 이슈 / 정리 대상

1. **`iam-core/` 소스 디렉터리 잔존** — 빌드/실행 라인에서는 빠졌으나 디렉터리는 참고용으로 보존 중. 안전하게 제거할 시점이 되면 `git rm -r iam-core` 한 번으로 정리 가능.
2. **`iam-ui/README.md`** 는 Vite 기본 템플릿 그대로 — 실제 내용 없음.
3. **시드된 sub-attribute 의 `canonicalValues` 미설정** — `emails.type` / `phoneNumbers.type` 등에 RFC 권장값 없음 (표준: `["work","home","other","unknown"]`). `ScimMetaInitializer` 갱신 후보.
4. **FE Attribute 화면이 새 PUT/DELETE 경로를 쓰는지 미확인** — 복합 PK 변경 후 (`/api/attributes/{domain}/{name}`) 호출 측 정렬 필요.
5. **사용자 목록 `UserTable` 이 실데이터 미연결** — 현재 목 데이터 사용 중. `/scim/v2/Users?filter=...` 연결 필요.
6. **부서 트리 `DeptTree` 이 실데이터 미연결** — `DepartmentService` API 호출로 교체 필요.

작업 시작 전 이 목록을 확인하고, 관련 영역 건드릴 때 함께 정리하는 것을 권장.
