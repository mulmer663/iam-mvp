# iam-core — Claude Code 컨텍스트 (⚠️ RETIRED)

> **이 모듈은 은퇴 처리되었습니다.** `settings.gradle` 과 `docker-compose.yml` 에서 제거되어 더 이상 빌드되거나 실행되지 않습니다.
> 소스 디렉터리는 마이그레이션 참고용으로만 보존 — 신규/수정 작업 모두 [`iam-registry`](../iam-registry/CLAUDE.md) 에서.

- 과거 거버넌스 문서: [AGENTS.md](./AGENTS.md) — 시점상 현재 엔진처럼 기술되어 있으나 실질은 MSA 이전 구조
- 루트 컨텍스트: [../CLAUDE.md](../CLAUDE.md)
- 대체 모듈: [../iam-registry/CLAUDE.md](../iam-registry/CLAUDE.md)

## 이 디렉터리를 건드려야 한다면

기본은 **건드리지 않는 것**. 굳이 필요한 경우라도:

- 코드를 추가하지 마세요 — `iam-registry` 에 추가
- 디렉터리 자체를 정리하려면 `git rm -r iam-core` 한 번이면 됩니다 (Gradle/Compose 에서 이미 빠짐)
- 마이그레이션이 끝났는지 확인하려면 `iam-registry/src/main/java/com/iam/registry/` 와 `iam-core/src/main/java/com/iam/core/` 의 도메인을 비교

`AGENTS.md` 의 규칙들은 historical reference 로만 유효합니다.
