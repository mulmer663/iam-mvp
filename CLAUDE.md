# IAM MVP — Claude Code 작업 가이드

> 이 문서는 Claude Code 가 자동 로드하는 루트 컨텍스트입니다.
> 도메인 상세는 [spec.md](./spec.md), 거버넌스/AI 에이전트 규칙은 [AGENTS.md](./AGENTS.md), 모듈 상세는 각 모듈의 `CLAUDE.md` / `AGENTS.md` 를 참조하세요.

## 프로젝트 한 줄 요약

**HR → IAM Registry → Target System** 데이터 파이프라인을 RabbitMQ 이벤트 choreography 로 분리하고, 변환 규칙을 Groovy 런타임 엔진에서 실행하는 **SCIM 2.0 기반 MSA IAM MVP**.

- Antigravity 로 개발 중 중단된 토이 프로젝트 (개인 실험)
- 상세 배경: [README.md](./README.md)

## 🎯 현재 집중 영역 (Now)

**SCIM 2.0 표준 스펙 충실 구현**. MSA 아키텍처, Groovy 룰 엔진, Envers 이력, UI 는 이미 뼈대가 잡혔으므로 **먼저 SCIM 2.0 프로토콜 적합성을 채우는 것이 우선 순위**입니다.

- 작업 시 `iam-registry` 의 SCIM 엔드포인트 / 스키마 / ResourceType / Filter / PATCH 처리 등 **RFC 7643, 7644 준수 여부**를 우선 검증
- 레퍼런스 스펙: 프로젝트 루트의 [`SCIM.txt`](./SCIM.txt) (RFC 원문 사본)
- 도메인/API 내부 계약: [`spec.md`](./spec.md) — 단, spec.md 가 표준과 어긋나는 부분이 있다면 **SCIM 표준을 우선** 하고 spec.md 를 업데이트

### 최종 목표 (Eventually, 지금은 보류)

궁극적 아키텍처 비전: [`archive/iam-architecture.docx`](./archive/iam-architecture.docx) — Universal Identity Fabric & Transformation Engine.
현재는 이 그림을 그대로 만들지 않고 **SCIM 표준 준수 완성** 이후에 재검토. 관련 작업 제안 시 "이건 SCIM 표준 범위가 맞는가?" 를 먼저 확인.

## 활성 모듈 맵 (✅ 현재 사용 / ⚠️ 레거시)

| 모듈 | 역할 | 상태 | 포트(Docker) |
|---|---|---|---|
| `iam-eureka` | Service Discovery (Spring Cloud Netflix) | ✅ | 8761 |
| `iam-registry` | SCIM 2.0 Identity Registry (Golden Record, JPA + Envers) | ✅ | 18084→18081 |
| `iam-engine` | Groovy 런타임 변환 엔진 (stateless, sandboxed) | ✅ | 18085→8080 |
| `iam-adapter-db` | DB Connectivity Adapter (RabbitMQ in/out, 순수 JDBC) | ✅ | 18086→8080 |
| `iam-ui` | Vue 3 + TS 프론트엔드 (Miller Columns UX) | ✅ | 15173→80 |
| `iam-core` | **레거시 — `iam-registry` 로 대체 진행 중** | ⚠️ | 18081→8081 |

> **주의:** `docker-compose.yml` 에는 존재하지 않는 `iam-connector-ad`, `iam-connector-hr` 서비스가 남아 있어 `docker-compose up` 실행 시 빌드 실패. 인프라만 띄우려면 `docker-compose.infra.yml` 사용.

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
| **풀 컨테이너** (현재 broken — phantom 모듈 있음) | `docker-compose up -d` |

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
- `iam-core` (또는 `iam-registry`) 는 커넥터 모듈에 **의존 금지** (단방향 이벤트만)
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
- [ ] 프로비저닝 **아웃바운드** 파이프라인 (진행 중)
- [ ] UI ↔ 백엔드 연동 완성

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

1. **`iam-core` 와 `iam-registry` 의 도메인 중복** — 동일 엔티티(`IamUser`, `SyncHistory`, `TransMapping` 등)가 양쪽에 존재. `iam-core` 는 레거시로 분류되어 있으나 정리 미완.
2. **`docker-compose.yml` phantom 서비스** — `iam-connector-ad`, `iam-connector-hr` 의 Dockerfile 이 없어 전체 빌드 실패.
3. **`iam-ui/README.md`** 는 Vite 기본 템플릿 그대로 — 실제 내용 없음.
4. **spec.md 의 Section 3 주석** — `iam-registry` 모듈 엔티티 참조 (정상) 이지만, `iam-core` 의 동일 엔티티와 혼동 주의.
5. **Spring Boot 버전 표기 불일치** — 일부 문서에 `3.5.8` 로 기재되어 있으나 실제 `build.gradle` 은 `3.4.3`.

작업 시작 전 이 목록을 확인하고, 관련 영역 건드릴 때 함께 정리하는 것을 권장.
