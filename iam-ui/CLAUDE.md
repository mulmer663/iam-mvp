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
├── api/                (client.ts, UserService.ts, HistoryService.ts, DepartmentService.ts)
├── components/
│   ├── common/         (StatusBadge, OperationBadge, UserProfileViewer)
│   ├── layout/         (AppSidebar)
│   ├── sync/           (HistoryTable/Card, SyncPipeline, AttributeMappingTable, RawPayloadViewer)
│   ├── attribute/      (AttributeForm)
│   ├── ui/             (Shadcn Vue 생성물 — Button, Badge, Sheet, Breadcrumb, Avatar …)
│   ├── DeptTree.vue
│   ├── UserDetailPane.vue
│   ├── UserTable.vue
│   └── ResourceTypeForm.vue
├── composables/        (useContainerWidth)
├── stores/             (miller.ts, attribute.ts, resourceType.ts)
├── types/              (scim.ts, attribute.ts, index.ts)
├── utils/              (theme.ts, date.ts, scim-metadata.ts, toast.ts)
├── views/              (Level 1~4 뷰: OrgUserManagement / UserChangeHistory / SyncHistory / SyncDetail 등)
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

## 참고

- `README.md` 는 Vite 기본 템플릿 — 실제 내용 없음 (정리 대상)
- Shadcn Vue 기반이지만 밀도 우선으로 override: `px-2`, `h-8` 등 사용
