# SCIM API 경계 원칙 (Golden Rule)

> **이 규칙은 프로젝트 전체에 적용되는 불변 원칙이다.**  
> 새 도메인 추가, 프론트 컴포넌트 작성, 백엔드 엔드포인트 설계 시 반드시 준수한다.

---

## 핵심 원칙

**백엔드는 항상 SCIM 2.0 표준 응답을 반환하고, 프론트엔드는 그 표준 API만 소비한다.**

---

## 백엔드 규칙

### 1. Identity 데이터는 `/scim/v2/` 하위에만 노출

Users, Groups, Departments 등 Identity 리소스는 반드시 SCIM 2.0 엔드포인트로만 제공한다.  
프론트 편의를 위한 커스텀 REST endpoint 추가 금지.

### 2. 응답 형식 준수 (RFC 7643 / RFC 7644)

**단일 리소스:**
```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
  "id": "...",
  "externalId": "...",
  "meta": { "resourceType": "User", "created": "...", "lastModified": "..." },
  "userName": "...",
  "...": "..."
}
```

**목록 응답:**
```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
  "totalResults": 10,
  "startIndex": 1,
  "itemsPerPage": 10,
  "Resources": [ ... ]
}
```

### 3. 도메인 속성은 flat map으로 포함

`ScimDynamicResource`의 `attributes` JSONB를 응답에 펼쳐서 반환한다.  
속성을 숨기거나 이름을 바꾸지 않는다.

---

## 프론트엔드 규칙

### 1. Identity 데이터는 `/scim/v2/` 엔드포인트만 호출

### 2. 응답 변환 함수로 필드를 드롭하지 않는다

```ts
// ❌ 금지 — SCIM 속성 유실
function toDept(d: any): Department {
    return { id: d.id, displayName: d.displayName } // deptCode, managerId 등 소멸
}

// ✅ 허용 — raw 그대로 사용
const res = await request<ScimListResponse<Department>>('/scim/v2/Departments')
return res.Resources
```

### 3. TypeScript 타입은 인프라 필드만 고정 정의

도메인 속성을 TypeScript 타입에 열거하면 스키마 변경 시 프론트 코드도 함께 수정해야 한다.

```ts
// ✅ 올바른 구조
export interface ScimResource {
    id: string
    externalId?: string
    schemas?: string[]
    meta?: { resourceType: string; created: string; lastModified: string }
    [key: string]: any  // 도메인 속성은 IamAttributeMeta 기반으로 동적 접근
}

export type Department = ScimResource
export type Group = ScimResource
// User는 SCIM 표준 필드가 많아 필요 시 extends 허용
```

### 4. 속성 메타데이터는 `/api/attributes` 사용 (예외 허용)

`IamAttributeMeta`는 SCIM이 정의하지 않는 IAM 전용 관리 데이터(UI 힌트, 표시명 등)이므로  
`/api/` 엔드포인트 사용을 허용한다.

---

## 경계 다이어그램

```
┌─────────────────────────────────────────────────────────┐
│                   SCIM 2.0 표준 영역                      │
│  /scim/v2/Users          /scim/v2/Schemas               │
│  /scim/v2/Groups         /scim/v2/ResourceTypes         │
│  /scim/v2/Departments    /scim/v2/ServiceProviderConfig │
└─────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────┐
│                   IAM 관리 API 영역                        │
│  /api/attributes    ← 속성 메타, UI 힌트 (비SCIM 허용)    │
│  /api/schemas       ← 스키마 CRUD                        │
│  /api/resource-types                                    │
└─────────────────────────────────────────────────────────┘
```

---

## 예외 처리

SCIM 표준으로 표현 불가한 기능(예: 부서 트리 조회, 벌크 작업)이 필요한 경우:
1. 먼저 SCIM filter/sort 파라미터로 해결 가능한지 검토
2. 불가능한 경우 `/api/` 하위에 추가하고 **이 파일에 예외 사유를 문서화**

---

*참고: RFC 7643 (SCIM Core Schema), RFC 7644 (SCIM Protocol)*
