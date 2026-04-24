# iam-adapter-db — Claude Code 컨텍스트

**역할:** "Dumb pipe" — 외부 DB 에서 데이터를 읽어 `RAW_INBOUND_DATA` 로 publish 하거나, `TARGET_OUTBOUND_DATA` 를 consume 하여 타겟 DB 에 반영. 비즈니스 로직/매핑 없음.

- 상세 규칙: [AGENTS.md](./AGENTS.md)
- 루트 컨텍스트: [../CLAUDE.md](../CLAUDE.md)

## 이 모듈에서 편집 시 기억할 것

- **매핑/변환 로직 금지** — 이 모듈은 `IamUser` 도, 타겟 스키마도 몰라야 함
- IAM 도메인 JPA 엔티티 / 어노테이션 **import 금지**
- `JdbcTemplate` + 동적 `DriverManagerDataSource` 사용 (페이로드 파라미터 기반 연결)
- `ResultSet` → `Map<String, Object>` / JSON 으로 변환 후 publish

## 주요 파일

```
com.iam.adapter.db
├── DbAdapterApplication
├── application/DynamicDbService
└── interfaces
    ├── messaging/OutboundDataListener   (TARGET_OUTBOUND_DATA consumer)
    └── rest/DbAdapterController         (수동 fetch 트리거 등)
```

## 흐름

- **Inbound:** 외부 DB fetch → `RAW_INBOUND_DATA` publish
- **Outbound:** `TARGET_OUTBOUND_DATA` consume → 타겟 DB 에 SQL 실행
