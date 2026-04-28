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

**SCIM 2.0 스키마 확장(Extension) 및 속성 메타 상세 스펙화** — 상세: [`.agent/rules/scim-schema-guide.md`](./.agent/rules/scim-schema-guide.md)

## 구현 상태

- [x] MSA 모듈 분리 / 서비스 디스커버리
- [x] RabbitMQ 이벤트 파이프라인
- [x] Groovy 런타임 룰 엔진 + 샌드박싱
- [x] SCIM 2.0 표준 API (Users / Schemas / ResourceTypes)
- [x] Core / Extension 하이브리드 스토리지 (Envers 이력 포함)
- [x] UI 사용자 등록/수정 — `DynamicUserForm` (속성 메타 기반)
- [ ] 프로비저닝 아웃바운드 파이프라인
- [ ] UI ↔ 백엔드 실데이터 연결 (UserTable / DeptTree)
- [ ] Department 도메인 구현 (`ScimDynamicResource` 기반)

## 알려진 이슈

1. **`UserTable`** 목 데이터 사용 중 — `/scim/v2/Users?filter=...` 연결 필요
2. **`DeptTree`** 목 데이터 사용 중 — `DepartmentService` API 연결 필요
3. **`iam-ui/README.md`** Vite 기본 템플릿 → 실제 내용으로 교체 필요

## 문서 네비게이션

| 찾는 것 | 파일 |
|---|---|
| 도메인 / API / 데이터 모델 | [spec.md](./spec.md) |
| 프로젝트 배경 | [README.md](./README.md) |
| AI 에이전트 거버넌스 | [AGENTS.md](./AGENTS.md) |
| SCIM 스키마 확장 가이드 | [.agent/rules/scim-schema-guide.md](./.agent/rules/scim-schema-guide.md) |
| Java 코드 스타일 | [.agent/rules/java-code-style-guide.md](./.agent/rules/java-code-style-guide.md) |
| Vue 코드 스타일 | [.agent/rules/vue-code-style-guide.md](./.agent/rules/vue-code-style-guide.md) |
| DynamicUserForm 패턴 | [.agent/rules/dynamic-form-guide.md](./.agent/rules/dynamic-form-guide.md) |
| 하이브리드 개발 워크플로우 | [.agent/workflows/hybrid-dev.md](./.agent/workflows/hybrid-dev.md) |
| EAM 모델 설계 메모 | [.agent/design/eam-model.md](./.agent/design/eam-model.md) |
| RFC 7643/7644 원문 | [SCIM.txt](./SCIM.txt) |
