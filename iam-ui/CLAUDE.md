# iam-ui — Claude Code 컨텍스트

**역할:** Vue 3 + TypeScript + Vite 프론트엔드. IAM Registry 의 SCIM 데이터를 고밀도 "IDE-like" Miller Columns UI 로 시각화.

- 상세 규칙: [AGENTS.md](./AGENTS.md)
- 루트 컨텍스트: [../CLAUDE.md](../CLAUDE.md)

## 이 모듈에서 편집 시 기억할 것

- **패키지 매니저: `pnpm`** (`npm`/`yarn` 혼용 금지)
- **`<script setup lang="ts">` 필수**, Options API 금지
- **빌드 검증:** 브라우저 확인 전 반드시 `pnpm run build` 통과 (`vue-tsc` 타입 체크 포함)
- **페이지 라우팅 대신 Miller Columns** — `stores/miller.ts` 의 `useMillerStore` 에 `MillerPane` 추가
- **아이콘:** `lucide-vue-next`
- **테마:** `@/utils/theme.ts` 의 `SYSTEM_THEMES` (Source=Blue, Integration=Purple, Audit=Amber, IAM=Orange)
- **밀도 디자인:** radius ≤ 4px, base font 13px, Neutral 팔레트, `h-8 / px-2` 톤

## 주요 디렉터리

```
src/
├── api/
│   ├── client.ts           (공통 fetch 래퍼)
│   ├── UserService.ts      (SCIM Users CRUD: createUser/patchUser/deleteUser + ScimPatchOp)
│   ├── HistoryService.ts
│   └── DepartmentService.ts
├── components/
│   ├── common/             (StatusBadge, OperationBadge, UserProfileViewer)
│   ├── layout/             (AppSidebar)
│   ├── sync/               (HistoryTable/Card, SyncPipeline, AttributeMappingTable, RawPayloadViewer)
│   ├── attribute/          (AttributeForm — display/canonicalValues/caseExact 포함)
│   ├── ui/                 (Shadcn Vue 생성물 — Button, Badge, Sheet, Breadcrumb, Avatar …)
│   ├── DeptTree.vue
│   ├── UserDetailPane.vue  (조회 + 편집 모드: DynamicUserForm + buildPatchOps)
│   ├── UserTable.vue
│   └── ResourceTypeForm.vue
├── composables/            (useContainerWidth)
├── stores/                 (miller.ts, attribute.ts, resourceType.ts)
├── types/
│   ├── index.ts            (User, Department, HistoryLog, MillerPane …)
│   ├── attribute.ts        (IamAttributeMeta — display/canonicalValues/referenceTypes/caseExact 포함)
│   └── scim.ts
├── utils/                  (theme.ts, date.ts, scim-metadata.ts, toast.ts)
├── views/
│   ├── OrgUserManagement.vue       (부서 트리 + 사용자 테이블 레이아웃)
│   ├── UserCreatePane.vue          (신규 사용자 등록 — DynamicUserForm 기반)
│   ├── DynamicUserForm.vue         (속성 메타 기반 동적 폼: display 필터, 타입별 위젯, 멀티밸류 flex-wrap)
│   ├── SchemaDetailPane.vue        (display=false 속성은 EyeOff 아이콘으로 표시)
│   ├── AttributeManagement.vue
│   ├── SyncHistory.vue / SyncDetail.vue
│   └── … (기타 관리 뷰)
├── mocks/
├── App.vue
└── main.ts
```

## 개발 명령

```bash
pnpm install
pnpm run dev            # Vite dev server
pnpm run build          # vue-tsc -b && vite build
pnpm run preview        # 빌드 결과 프리뷰
```

## 동적 폼 (DynamicUserForm) 패턴

`IamAttributeMeta` 배열을 받아 UI를 자동 생성하는 원칙:

- `display === false` 인 속성은 렌더링에서 제외
- `mutability === 'READ_ONLY'` / `'IMMUTABLE'` → 항상 비활성
- `mutability === 'WRITE_ONCE'` → 편집 모드에서만 비활성
- 위젯 결정 순서: `BOOLEAN` → Checkbox / `canonicalValues` 있음 → Select / `uiComponent === 'textarea'` → Textarea / 나머지 → Input
- 멀티밸류 COMPLEX (emails, phoneNumbers 등): flex-wrap 행, 셀 너비는 타입/canonicalValues/uiComponent 기반 — **속성 이름 하드코딩 금지**
- `addresses.formatted` 는 `uiComponent='textarea'` 로 시드 — 전체 행 차지

## reka-ui Checkbox 바인딩 주의

reka-ui v2.x 는 `:model-value` / `@update:model-value` 사용. `:checked` / `@update:checked` 는 동작 안 함.

```html
<Checkbox :model-value="formData.someFlag ?? true"
          @update:model-value="(v: any) => formData.someFlag = !!v" />
```

## 참고

- `README.md` 는 Vite 기본 템플릿 — 실제 내용 없음 (정리 대상)
- Shadcn Vue 기반이지만 밀도 우선으로 override: `px-2`, `h-8` 등 사용
