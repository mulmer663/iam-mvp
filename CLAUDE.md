# IAM MVP — Claude Code 작업 가이드

> 도메인 상세 → [spec.md](./spec.md) / 거버넌스 → [AGENTS.md](./AGENTS.md) / 모듈별 규칙 → 각 모듈 `CLAUDE.md`

## 프로젝트 한 줄 요약

**HR → IAM Registry → Target System** 파이프라인을 RabbitMQ 이벤트 choreography로 분리하고, 변환 규칙을 Groovy 런타임 엔진에서 실행하는 **SCIM 2.0 기반 MSA IAM MVP**.

## 활성 모듈

| 모듈 | 역할 | 포트(Docker) |
|---|---|---|
| `iam-eureka` | Service Discovery | 8761 |
| `iam-registry` | SCIM 2.0 Identity Registry (JPA + Envers) | 18081 |
| `iam-engine` | Groovy 런타임 변환 엔진 (stateless) | 18085→8080 |
| `iam-adapter-db` | DB Connectivity Adapter (RabbitMQ in/out) | 18086→8080 |
| `iam-ui` | Vue 3 + TS 프론트엔드 (Miller Columns UX) | 15173→80 |

## 자주 쓰는 명령어

```bash
# 백엔드
./gradlew build
./gradlew :iam-registry:bootRun
./gradlew :iam-registry:test

# 프론트엔드
cd iam-ui && pnpm install && pnpm run dev

# 인프라만 Docker (권장)
docker-compose -f docker-compose.infra.yml up -d
```

## 현재 집중 영역

**프로비저닝 아웃바운드 파이프라인 + Department 도메인 완성**

## 구현 상태

### 인프라 / 백엔드
- [x] MSA 모듈 분리 / 서비스 디스커버리
- [x] RabbitMQ 이벤트 파이프라인
- [x] Groovy 런타임 룰 엔진 + 샌드박싱
- [x] SCIM 2.0 표준 API — Users / Groups / Schemas / ResourceTypes
- [x] Core / Extension 하이브리드 스토리지 (Envers 이력 포함)
- [x] Schema 삭제 카스케이드 (RT 확장 분리 → 속성 삭제 → 스키마 삭제)
- [ ] Department SCIM API (`/scim/v2/Departments`) — `GenericScimController` 라우팅 확인 필요
- [ ] 프로비저닝 아웃바운드 파이프라인 (iam-registry → RabbitMQ → iam-adapter-db → AD)

### UI (iam-ui)
- [x] Miller Columns 반응형 너비 토큰 시스템 (w1/w2/w3) — `useMillerSizes.ts`
- [x] 사용자 CRUD — `DynamicUserForm` + `UserDetailPane` (속성 메타 기반 동적 렌더링)
- [x] Group CRUD + GroupMembersPane (멤버 추가/제거)
- [x] Department CRUD + DeptMembersPane (부서 멤버 목록)
- [x] Attribute / Schema / ResourceType 관리 UI
- [x] SCIM 편집 가능성 단일 진원지 — `scim-permissions.ts`
- [x] SchemaCreateForm 3단계 위저드 (RT → CORE/Extension → URN)
- [ ] UserTable 실 API 연결 — `/scim/v2/Users` (현재 `UserService.getUsers()` 호출 중이나 필터/페이징 미구현)
- [ ] SyncHistory / UserChangeHistory 실 데이터 검증

## 알려진 이슈

1. **Department SCIM API** — `GenericScimController`가 `/scim/v2/Departments`를 동적 라우팅하지만 실제 동작 검증 필요
2. **UserTable 페이징/필터** — `GET /scim/v2/Users` 연결은 됐으나 `filter=`, `startIndex=`, `count=` 파라미터 미구현
3. **`iam-ui/README.md`** — Vite 기본 템플릿 → 실제 내용으로 교체 필요

## 문서 네비게이션

| 찾는 것 | 파일 |
|---|---|
| 도메인 / API / 데이터 모델 | [spec.md](./spec.md) |
| 프로젝트 배경 | [README.md](./README.md) |
| AI 에이전트 거버넌스 | [AGENTS.md](./AGENTS.md) |
| **⚠️ SCIM API 경계 원칙 (Golden Rule)** | [.agent/rules/scim-api-boundary-rule.md](./.agent/rules/scim-api-boundary-rule.md) |
| SCIM 스키마 확장 가이드 | [.agent/rules/scim-schema-guide.md](./.agent/rules/scim-schema-guide.md) |
| **SCIM 편집 가능성 매트릭스 (Editability)** | [.agent/rules/scim-permissions-guide.md](./.agent/rules/scim-permissions-guide.md) |
| Java 코드 스타일 | [.agent/rules/java-code-style-guide.md](./.agent/rules/java-code-style-guide.md) |
| Vue 코드 스타일 | [.agent/rules/vue-code-style-guide.md](./.agent/rules/vue-code-style-guide.md) |
| DynamicUserForm 패턴 | [.agent/rules/dynamic-form-guide.md](./.agent/rules/dynamic-form-guide.md) |
| Miller Columns 패턴 & 너비 토큰 | [.agent/rules/miller-columns-guide.md](./.agent/rules/miller-columns-guide.md) |
| 하이브리드 개발 워크플로우 | [.agent/workflows/hybrid-dev.md](./.agent/workflows/hybrid-dev.md) |
| EAM 모델 설계 메모 | [.agent/design/eam-model.md](./.agent/design/eam-model.md) |
| RFC 7643/7644 원문 | [SCIM.txt](./SCIM.txt) |
