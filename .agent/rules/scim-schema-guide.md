---
trigger: glob
globs: "iam-registry/src/**/*.java"
---

# SCIM 2.0 스키마 & 속성 메타 가이드

## 확장 인프라 엔티티

| 엔티티 | 역할 |
|---|---|
| `ScimSchemaMeta` | 스키마 URN 레지스트리. 속성 리스트 보유 |
| `IamAttributeMeta` | 속성 단위 메타 — RFC 7643 특성 + 프로젝트 확장(`targetDomain`, `category`, `adminOnly`, `uiComponent`, `display`) |
| `ScimResourceTypeMeta` | ResourceType 정의 (id/endpoint/main schema + 확장 스키마 목록) |
| `ScimResourceTypeExtension` | ResourceType ↔ Extension 스키마 연결 (required 여부) |
| `ScimDynamicResource` | 런타임 등록된 리소스 타입의 실제 저장 (JSONB `attributes`) |
| `IamUserExtension` / `ExtensionData` | User 확장 속성 JSONB — `Map<URN, ExtensionData>` |

## 이 영역 편집 시 체크리스트

1. **RFC 7643 준수** — `IamAttributeMeta` 핵심 필드(`caseExact` / `canonicalValues` / `referenceTypes` / `display`) 유지
2. `/Schemas`, `/ResourceTypes`, `/ServiceProviderConfig` 디스커버리 응답이 메타 테이블에서 **정확히 재구성**되는지 확인
3. **확장 속성 PATCH** — `"schemas"` 배열에 확장 URN 포함, 값 타겟팅 `urn:...:User:department` 형태 유지
4. **동적 ResourceType CRUD** (`ScimDynamicResource`) 가 표준 `/scim/v2/{ResourceType}` 규약을 따르는지
5. 고객사 확장 추가 시 **코드 변경 없이 DB 만으로** — `POST /api/schemas` + `POST /api/attributes` 만으로 즉시 반영
6. 표준과 spec.md 충돌 시 **표준 우선** 후 spec.md 갱신

## 레퍼런스

- RFC 원문: [`SCIM.txt`](../../SCIM.txt) (RFC 7643/7644)
- 내부 계약: [`spec.md`](../../spec.md) §6.6, §6.7
