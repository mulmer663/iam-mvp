# Department UI 재설계 계획

> 작성일: 2026-04-28  
> 관련 이슈: (1) Miller 컬럼 클릭 과다 (2) 부서 속성 스키마 미연동

---

## 현황 분석

### 현재 구조

```
AppSidebar → "Departments" 클릭
  └─ DeptManagement.vue (paneIndex=0, deptId=undefined)
       ├─ 상단: currentDept 상세 (deptId 있을 때)
       │    └─ UserProfileViewer (도메인 무관 속성 표시 — 버그)
       └─ 하단: subDepts 리스트
            └─ 클릭 → DeptManagement (paneIndex+1, deptId=선택한dept)
```

**문제 1 — 클릭 과다:**  
detail + sub-list가 한 pane에 혼재. level 2 부서(예: IT-SECOPS) 상세 보려면  
루트 → GLOBAL-IT → IT-SEC → IT-SECOPS, 총 **4번 클릭** 필요.

**문제 2 — 속성 미연동:**  
`UserProfileViewer.getMeta(key)`가 `attributeStore.getAttributeByCode(key)`를 호출하는데  
이 getter가 domain 구분 없이 첫 번째 매칭을 반환함.  
→ `displayName`은 USER domain 메타로 매핑, `deptCode`·`managerId` 등 신규 속성은 아예 못 찾음.

```ts
// stores/attribute.ts — 현재 버그 위치
getAttributeByCode: (state) => (name: string) =>
    state.attributes.find(attr => attr.name === name)  // ← domain 무시
```

---

## 목표 구조

```
LVL.1 — DeptManagement.vue (컨테이너)   LVL.2 — DeptDetailPane.vue
  ┌─────────────────────────────────┐     ┌──────────────────────────────┐
  │ [검색창]                         │     │ GLOBAL-IT                    │
  │                                 │     │ ID: GLOBAL-IT  EXT: EXT-001  │
  │ ▶ GLOBAL-IT            →선택→  │ ──► │                              │
  │   ├─ Infrastructure             │     │ ── 속성 (스키마 기반) ──      │
  │   ├─ Cyber Security             │     │ deptCode     GITD            │
  │   │   └─ Sec Ops                │     │ level        0               │
  │   └─ Biz Apps                   │     │ manager      James Kang      │
  │ ▶ Internal Audit                │     │ costCenter   CC-1000         │
  │ ▶ SAP HR Division               │     │ email        it@corp.local   │
  │   ├─ HR Planning                │     │ ...                          │
  │   └─ HR Operations              │     │                              │
  │ ▶ External Vendors              │     │ [VIEW MEMBERS]               │
  └─────────────────────────────────┘     └──────────────────────────────┘
```

- **LVL.1**: 전체 트리를 한눈에 표시, 클릭 1회로 detail 오픈
- **LVL.2**: 스키마 기반 속성 표시, Members 이동 버튼

---

---

## ⚠️ GOLDEN RULE — SCIM 인터페이스 경계 원칙

> 이 규칙은 이 문서뿐 아니라 **프로젝트 전체**에 적용되는 불변 원칙이다.

### 규칙

**백엔드는 항상 SCIM 2.0 표준 응답을 반환하고, 프론트엔드는 그 표준 API만 소비한다.**

### 백엔드 책임

| 항목 | 규칙 |
|---|---|
| Identity 데이터 엔드포인트 | 반드시 `/scim/v2/` 하위에 위치 |
| 단일 리소스 응답 | `id`, `schemas`, `meta` 필드 포함 (RFC 7643 §3.1) |
| 목록 응답 | `schemas`, `totalResults`, `startIndex`, `itemsPerPage`, `Resources` 포함 (RFC 7644 §3.4.2) |
| 도메인 속성 | 타입별 SCIM Schema URN으로 식별되는 flat 또는 extension 구조로 반환 |
| 프론트 편의용 커스텀 endpoint | **금지** — SCIM 표준으로 표현 불가한 경우에만 예외, 반드시 문서화 |

### 프론트엔드 책임

| 항목 | 규칙 |
|---|---|
| Identity 데이터 접근 | `/scim/v2/` 엔드포인트만 사용 |
| 타입 매핑 함수 | SCIM 응답 필드를 **제거하거나 재구조화하는 변환 금지** |
| TypeScript 타입 | 인프라 필드(`id`, `externalId`, `meta`, `schemas`)만 고정 정의 — 도메인 속성은 `[key: string]: any` |
| 속성 메타데이터 | `/api/attributes` (IAM 관리 API) 사용 — SCIM Identity 데이터가 아니므로 예외 허용 |

### 경계 구분

```
[SCIM 2.0 표준 영역]            [IAM 관리 영역]
  /scim/v2/Users                  /api/attributes    ← 속성 메타, UI 힌트
  /scim/v2/Groups                 /api/schemas       ← 스키마 CRUD
  /scim/v2/Departments            /api/resource-types
  /scim/v2/Schemas   (discovery)
  /scim/v2/ResourceTypes
```

Identity 데이터(`Users`, `Groups`, `Departments`)는 SCIM 경계 안에서만 다룬다.  
속성 메타(`IamAttributeMeta`)는 SCIM이 정의하지 않는 IAM 관리 데이터이므로 `/api/` 사용.

### 위반 사례 (현재 코드)

```ts
// ❌ DepartmentService.ts — SCIM 응답 필드를 드롭하는 변환 함수
function toDepartment(d: ScimDepartmentResponse): Department {
    return {
        id: d.id,
        displayName: d.displayName,  // deptCode, managerId 등 유실
        ...
    }
}

// ✅ 올바른 방식 — raw 응답 그대로 사용
const res = await request<ScimListResponse<Department>>('/scim/v2/Departments')
return res.Resources  // 백엔드가 내려준 속성 전부 보존
```

---

## 타입 설계 원칙 — 동적 속성 처리

**TypeScript 타입에 도메인 속성을 고정하지 않는다.**

백엔드 `ScimDynamicResourceService.toScimResponse()`는 `attributes` JSONB 컬럼 전체를
flat map으로 응답한다. 속성 추가/삭제는 `IamAttributeMeta` 시드 변경만으로 완결되어야 하며,
프론트 타입 파일을 건드려야 하는 순간 스키마 기반 시스템의 의미가 없어진다.

### SCIM 인프라 필드 vs 도메인 속성

| 종류 | 정의 위치 | 예시 |
|---|---|---|
| SCIM 인프라 | TypeScript 타입 | `id`, `externalId`, `meta`, `schemas` |
| 도메인 속성 | `IamAttributeMeta` (DB) | `displayName`, `deptCode`, `managerId`, ... |

### 1. `types/index.ts` — Department 타입 슬림화

인프라 필드만 남기고, 도메인 속성은 인덱스 시그니처로 열어둔다:

```ts
export interface Department {
    id: string
    externalId?: string
    meta?: { resourceType: string; created: string; lastModified: string }
    [key: string]: any   // 도메인 속성은 IamAttributeMeta 기반으로 동적 접근
}
```

### 2. `api/DepartmentService.ts` — raw 응답 그대로 전달

`toDepartment()` 매핑 함수 제거. 백엔드 응답을 그대로 `Department`로 캐스팅:

```ts
// 변경 전: 고정 필드만 추출 → deptCode, managerId 등 유실
function toDepartment(d: ScimDepartmentResponse): Department { ... }

// 변경 후: raw 응답 그대로 사용
async function fetchAll(): Promise<Department[]> {
    const res = await request<ScimListResponse<Department>>('/scim/v2/Departments')
    _cache = res.Resources
    return _cache
}
```

`ScimDepartmentResponse` 인터페이스도 삭제 (타입 중복 제거).

### 3. `DeptManagement.vue` — 컨테이너로 전환

**현재:** detail + sub-list 혼재, 재귀 Miller 컬럼  
**변경:** DeptTree 전체 + 선택 시 DeptDetailPane을 LVL.2로 오픈

```
변경 전                          변경 후
─────────────────────────────   ─────────────────────────────
props: { deptId?, paneIndex }   props: { paneIndex }
상단: currentDept detail         전체 트리 (DeptTree)
하단: subDepts 리스트            선택 dept → openDeptDetail()
재귀 Miller 구조                 단순 컨테이너, 재귀 없음
```

- `DeptService.getAllDepartments()` 한 번 호출, DeptTree에 전달
- 선택한 deptId를 `selectedDeptId` ref로 관리
- `openDeptDetail(deptId)` → `millerStore.setPane(paneIndex+1, DeptDetailPane)`

### 4. `DeptDetailPane.vue` — 신규 생성

부서 상세 전용 pane. `schemaAttributes` 패턴(`SchemaDetailPane.vue` 참조)을 참고해  
`attrStore.deptAttributes`를 기반으로 속성을 렌더링.

```
props: { deptId, paneIndex }

layout:
  ┌─ 헤더 ──────────────────────────────┐
  │ [Network 아이콘]  displayName        │
  │                  ID / externalId    │
  │                  active badge       │
  ├─ 속성 섹션 (스키마 기반) ─────────────┤
  │ deptAttributes 순서대로 key-value   │
  │ displayName은 meta.displayName 사용 │
  │ READ_ONLY 속성은 잠금 아이콘        │
  ├─ 액션 ──────────────────────────────┤
  │ [VIEW MEMBERS]  [Edit]              │
  └──────────────────────────────────────┘
```

**속성 렌더링 로직:**

```ts
const deptAttrs = computed(() =>
    attrStore.deptAttributes.filter(a => !a.parentName)  // root attrs only
)

// dept 데이터와 메타를 매핑
const rows = computed(() =>
    deptAttrs.value.map(meta => ({
        meta,
        value: dept.value?.[meta.name] ?? null
    }))
)
```

### 5. `stores/attribute.ts` — getMeta 도메인 필터 추가

```ts
// 추가할 getter
getAttributeByCodeAndDomain: (state) =>
    (name: string, domain: AttributeTargetDomain) =>
        state.attributes.find(a => a.name === name && a.targetDomain === domain)
```

`UserProfileViewer`에 optional `domain` prop 추가해 기존 User 뷰어는 그대로 동작,  
Department에서 호출 시 `domain="DEPARTMENT"` 전달.

> **참고:** `UserProfileViewer`는 SyncHistory 등에서도 사용 중이라 breaking change 없이 optional로 처리.

---

## 영향 범위

| 파일 | 변경 유형 | 비고 |
|---|---|---|
| `types/index.ts` | 수정 | Department 인터페이스 슬림화 (인프라 필드 + 인덱스 시그니처) |
| `api/DepartmentService.ts` | 수정 | toDepartment() 제거, raw 응답 그대로 사용 |
| `views/DeptManagement.vue` | 대폭 수정 | 컨테이너로 전환, DeptTree 위임 |
| `views/DeptDetailPane.vue` | 신규 | 스키마 기반 부서 상세 |
| `stores/attribute.ts` | 수정 | domain-aware getter 추가 |
| `components/common/UserProfileViewer.vue` | 소폭 수정 | optional domain prop |
| `components/DeptTree.vue` | 무변경 | 재사용 |
| `views/OrgUserManagement.vue` | 무변경 | DeptTree 사용 방식 동일 |

---

## 비변경 사항

- `DeptTree.vue` 컴포넌트 로직 — 재사용
- `OrgUserManagement.vue` — DeptTree 사용 방식 동일
- Miller store API — `setPane` 그대로 사용
- 백엔드 API — 변경 없음
- `openMemberSync` 로직 — DeptDetailPane으로 이동

---

## 검증 기준

- [ ] 루트 부서 전체가 트리로 한 번에 보임
- [ ] 부서 클릭 1회로 LVL.2 detail pane 오픈
- [ ] DeptDetailPane에 12개 속성이 스키마 순서대로 표시
- [ ] READ_ONLY 속성(`level`, `managerDisplayName`)에 잠금 표시
- [ ] 기존 OrgUserManagement DeptTree 필터 정상 동작
- [ ] `UserProfileViewer` 기존 사용처(SyncHistory 등) 정상 동작
