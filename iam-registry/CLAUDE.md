# iam-registry — Claude Code 컨텍스트

**역할:** SCIM 2.0 Identity Registry — Golden Record 저장소 및 표준 REST API 제공자.

- 상세 규칙: [AGENTS.md](./AGENTS.md)
- 루트 컨텍스트: [../CLAUDE.md](../CLAUDE.md)
- 도메인/API 명세: [../spec.md](../spec.md)

## 이 모듈에서 편집 시 기억할 것

- **입력:** RabbitMQ `CDM_DATA_QUEUE` 만 consume (transform 은 `iam-engine` 에서 끝남)
- **출력:** SCIM 2.0 REST API + Envers 기반 이력
- **PK:** Hypersistence `TSID` 필수 (spec.md 와 일치)
- **traceId:** `IamUser`, `SyncHistory` 변경 시 반드시 전파 — `CustomRevisionEntity` 경유
- **경계:** 외부 타겟 DB 직접 질의 금지 → RabbitMQ outbound 로만 전달 (`iam-adapter-db` 담당)
- **테스트:** `./gradlew :iam-registry:test`

## 주요 패키지 구조

```
com.iam.registry
├── RegistryApplication
├── application        (UserRegistryService 등)
├── domain
│   ├── user           (IamUser, IamUserExtension, IdentityLink, EnterpriseUserExtension)
│   ├── scim           (ScimSchemaMeta, ScimResourceTypeMeta, ScimDynamicResource, IamAttributeMeta)
│   ├── sync           (SyncHistory, TransMapping, TransRuleMeta, TransCodeMeta/Value, SourceSystem)
│   └── common         (ExtensionData, GenericExtension, 상수)
├── infrastructure     (RabbitMQConfig)
└── interfaces
    ├── messaging      (CdmDataListener)
    └── rest           (UserRegistryController)
```

> `iam-core` 모듈에 동일 이름 엔티티가 있으나 **레거시**. 신규 작업은 모두 `iam-registry` 에서.
