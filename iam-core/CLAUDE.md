# iam-core — Claude Code 컨텍스트 (⚠️ LEGACY)

> **이 모듈은 레거시입니다.** 신규 도메인 작업은 모두 [`iam-registry`](../iam-registry/CLAUDE.md) 에서 진행합니다.
>
> `IamUser`, `SyncHistory`, `TransMapping` 등 주요 엔티티가 `iam-registry` 에 동일 이름으로 재구현되어 있으며, 향후 이 모듈은 정리 대상입니다.

- 상세(레거시) 규칙: [AGENTS.md](./AGENTS.md) — 시점상 현재 엔진처럼 기술되어 있으나 실질은 MSA 이전 구조
- 루트 컨텍스트: [../CLAUDE.md](../CLAUDE.md)
- 대체 모듈: [../iam-registry/CLAUDE.md](../iam-registry/CLAUDE.md)

## 이 모듈을 건드려야 한다면

- 새 기능 추가는 **금지** — `iam-registry` 에 추가
- 버그 수정이 불가피하면 **같은 내용을 `iam-registry` 에도 반영**할지 판단 필요
- 제거 순서 검토:
  1. `docker-compose.yml` 의 `iam-core` 서비스 정리
  2. `settings.gradle` 에서 모듈 제외
  3. 소스 삭제

`AGENTS.md` 는 그대로 두되, 이 모듈의 규칙을 따라야 하는 경우는 "현상 유지 / 레거시 버그 수정" 에 한정.
