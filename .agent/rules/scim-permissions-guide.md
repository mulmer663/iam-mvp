# SCIM 편집 가능성 매트릭스 (Editability Policy)

> RFC 7643/7644 + 프로젝트 정책 단일 진원지.
> 코드 진원지: [`iam-ui/src/utils/scim-permissions.ts`](../../iam-ui/src/utils/scim-permissions.ts)

## 핵심 원칙

1. **RFC 표준은 잠금** — User/Group ResourceType, RFC 정의 Schema(`urn:ietf:params:scim:schemas:core:2.0:*`, `…:extension:enterprise:2.0:*`), 그리고 그 안의 속성은 메타 수정/삭제 불가
2. **확장은 항상 허용** — RFC §3.3 ("Schema extensions are additive"). 표준 RT에도 `schemaExtensions` 부착은 SCIM의 본래 의도
3. **Extension Schema 자체는 자유롭게** — 사용자 정의 Extension Schema의 메타/속성/삭제는 모두 가능
4. **단일 진원지** — UI 가드는 반드시 `scim-permissions.ts`의 헬퍼만 사용. inline `id === 'User'` 같은 체크 금지

---

## 식별 기준

| 분류 | 판별 함수 | 기준 |
|---|---|---|
| **표준 ResourceType** | `isStandardResourceType(id)` | id ∈ `{ "User", "Group" }` (RFC 7643 §4.1, §4.2) |
| **표준 Schema** | `isStandardSchema(uri)` | URI prefix가 `urn:ietf:params:scim:schemas:core:2.0:` 또는 `urn:ietf:params:scim:schemas:extension:enterprise:2.0:` |
| **표준 Attribute** | `isStandardAttribute(attr)` | `attr.scimSchemaUri`가 표준 Schema |

---

## 권한 매트릭스

### Schema (`schemaCapabilities(schema)`)

| 동작 | RFC 표준 Schema | 사용자 정의 Schema |
|---|:---:|:---:|
| `canEditMeta` (이름/설명) | ❌ | ✅ |
| `canDelete` | ❌ | ✅ |
| `canAddAttribute` | ❌ | ✅ |
| `canRemoveAttribute` | ❌ | ✅ |

> **주의:** RFC 표준 Schema에는 새 속성을 추가할 수 없습니다. 추가 속성이 필요하면 별도 Extension Schema를 만들어 ResourceType에 부착하세요.

### ResourceType (`resourceTypeCapabilities(rt)`)

| 동작 | RFC 표준 RT (User/Group) | 사용자 정의 RT |
|---|:---:|:---:|
| `canEditMeta` (name/description/endpoint) | ❌ | ✅ |
| `canDelete` | ❌ | ✅ |
| **`canEditExtensions`** | ✅ | ✅ |

> **이게 핵심:** User/Group RT의 기본 메타는 잠그지만, `schemaExtensions` 추가/제거는 항상 허용. RFC §3.3가 정의한 SCIM 확장의 본래 사용처입니다.

### Attribute (`attributeCapabilities(attr)`)

| 동작 | 표준 Schema 내 속성 | 사용자 정의 Schema 내 속성 |
|---|:---:|:---:|
| `canEdit` | ❌ | ✅ |
| `canDelete` | ❌ | ✅ |

---

## RFC 근거 인용

**RFC 7643 §3.3 — Attribute Extensions to Resources**
> SCIM allows resource types to have extensions in addition to their core schema. … all extensions are additive (similar to the LDAP auxiliary object class). Each value in the "schemas" attribute indicates additive schema that MAY exist in a SCIM resource representation. … Schema extensions SHOULD avoid redefining any attributes defined in this specification.

**RFC 7643 §6 — ResourceType Schema**
> Resource type resources are READ-ONLY (via SCIM API). [관리자 admin API에서는 수정 가능 — 이 프로젝트의 `/api/resource-types`]

**RFC 7643 §7 — Schema Definition**
> Schema resources are not modifiable, and their associated attributes have a mutability of "readOnly" (via SCIM API).

> ⚠️ "READ-ONLY"는 SCIM 표준 API 클라이언트(외부 SCIM 클라이언트) 관점입니다. 우리 프로젝트의 admin UI(`/api/schemas`, `/api/resource-types`)는 관리자가 메타를 정의/조정하는 백오피스 채널이므로 수정을 허용합니다 — 단 RFC 정의 부분은 정합성 유지를 위해 잠급니다.

---

## Schema 등록 흐름 (UI)

신규 Extension Schema를 만들 때 사용자는 다음 순서로 결정합니다:

```
1. Target ResourceType 선택   (예: User, Group, Department)
   ↓
2. 확장 유형 선택
   ├─ "Add to CORE" — 해당 RT의 기본 스키마에 속성 직접 추가
   │     (사용자 정의 RT일 때만 가능. User/Group의 CORE는 RFC 잠금)
   │     → 새 Schema 생성 없이 attribute만 추가됨
   │
   └─ "Create Extension" — 새 Extension Schema URN 생성
         (모든 RT에서 가능. 표준 RT 포함)
         → SchemaCreateForm: URN/name/description 입력
         → 자동으로 해당 RT의 schemaExtensions 에 부착
   ↓
3. Attribute 추가 (별도 단계)
```

### 가능 / 불가능 매트릭스

| Target RT | "Add to CORE" 가능? | "Create Extension" 가능? |
|---|:---:|:---:|
| User (RFC) | ❌ (CORE 잠금) | ✅ |
| Group (RFC) | ❌ (CORE 잠금) | ✅ |
| Department (사용자 정의) | ✅ | ✅ |
| 신규 사용자 정의 RT | ✅ | ✅ |

---

## 적용 패턴 (코드)

### ✅ Do — 헬퍼 사용

```ts
import { resourceTypeCapabilities } from '@/utils/scim-permissions'

const caps = computed(() => resourceTypeCapabilities(props.resourceTypeId))
// template
<input :disabled="!caps.canEditMeta" />
<Button v-if="caps.canDelete" @click="onDelete" />
```

### ❌ Don't — inline 체크

```ts
// 정책이 코드 곳곳에 흩어지면 또 헷갈립니다
const isStandard = props.id === 'User' || props.id === 'Group'
<input :disabled="isStandard" />
```

---

## 갱신 시 절차

이 정책을 바꿔야 하는 상황(예: RFC 7644 PATCH 정책 추가, 새 표준 RT 등장):
1. `scim-permissions.ts`에서 헬퍼 시그니처/반환값 변경
2. 영향받는 capabilities 객체 boolean 추가
3. 이 문서의 매트릭스 표 업데이트
4. RFC 인용이 새로 추가/수정되면 §번호 정확히 명기
