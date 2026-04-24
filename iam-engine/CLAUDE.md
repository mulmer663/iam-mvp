# iam-engine — Claude Code 컨텍스트

**역할:** Stateless Groovy 변환 엔진 — RabbitMQ 페이로드를 읽어 SCIM 표준 구조(`CDM_DATA`)로 변환하거나, 변경 이벤트를 타겟 시스템 페이로드(`TARGET_OUTBOUND_DATA`)로 변환.

- 상세 규칙: [AGENTS.md](./AGENTS.md)
- 루트 컨텍스트: [../CLAUDE.md](../CLAUDE.md)

## 이 모듈에서 편집 시 기억할 것

- **DB 접근 금지** — 모든 입출력은 RabbitMQ
- **상태 유지 금지** — horizontal scale 가능해야 함 (예외: Groovy 스크립트 컴파일 캐시)
- **샌드박스 필수** — `SecureASTCustomizer` 없이 Groovy 실행 금지
  - 금지 클래스: `System`, `Runtime`, `ProcessBuilder`, reflection, file IO
  - 허용 화이트리스트: `String`, `Math`, `Date` 등
- **하드코딩 금지** — HR/AD 특정 규칙은 DB 에 저장되어 페이로드로 주입됨
- 스크립트는 SHA-256 해시 + `IAM_TRANS_RULE_VERSION` 버전 관리

## 주요 파일

```
com.iam.engine
├── EngineApplication
├── application/GroovyScriptEngineService
├── config/GroovySandboxConfig        (SecureASTCustomizer 설정)
└── interfaces/messaging/EngineDataListener
```
