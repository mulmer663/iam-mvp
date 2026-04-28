# iam-registry — Claude Code 컨텍스트

**역할:** SCIM 2.0 Identity Registry — Golden Record 저장소 및 표준 REST API 제공자.

- 루트 컨텍스트: [../CLAUDE.md](../CLAUDE.md)
- 도메인/API 명세: [../spec.md](../spec.md)
- SCIM 스키마 가이드: [../.agent/rules/scim-schema-guide.md](../.agent/rules/scim-schema-guide.md)

## 편집 시 경계 규칙

- **입력:** RabbitMQ `CDM_DATA_QUEUE` 만 consume (transform 은 `iam-engine` 에서 끝남)
- **출력:** SCIM 2.0 REST API + Envers 기반 이력
- **PK:** Hypersistence TSID 필수
- **traceId:** `IamUser`, `SyncHistory` 변경 시 반드시 전파 — `CustomRevisionEntity` 경유
- **경계:** 외부 타겟 DB 직접 질의 금지 → RabbitMQ outbound 로만 전달
- **테스트:** `./gradlew :iam-registry:test`
