# iam-ui

Vue 3 + TypeScript 기반 IAM 관리 콘솔. SCIM 2.0 Identity Registry의 데이터를 고밀도 Miller Columns UI로 시각화합니다.

## 기술 스택

| 항목 | 선택 |
|---|---|
| Framework | Vue 3 (`<script setup lang="ts">`) |
| Build | Vite |
| Language | TypeScript |
| Styling | Tailwind CSS + Shadcn Vue |
| Icons | lucide-vue-next |
| State | Pinia |
| Package Manager | pnpm |

## 개발 명령

```bash
pnpm install        # 의존성 설치
pnpm run dev        # Vite dev server (http://localhost:5173)
pnpm run build      # vue-tsc -b && vite build (타입 체크 포함)
pnpm run preview    # 빌드 결과 로컬 프리뷰
```

## 백엔드 연결

`iam-registry` (port **18081**) 를 직접 호출합니다.

```
http://localhost:18081/scim/v2/Users
http://localhost:18081/scim/v2/Schemas
http://localhost:18081/api/attributes
http://localhost:18081/api/schemas
```

## 주요 화면 및 라우팅

페이지 라우팅 대신 **Miller Columns** 방식을 사용합니다. `stores/miller.ts`의 `useMillerStore`에 `MillerPane`을 추가하여 패널을 확장합니다.

| 뷰 | 설명 |
|---|---|
| `OrgUserManagement` | 사용자 목록 + 상세 + 이력 (4-depth Miller) |
| `AttributeManagement` | SCIM 속성 메타 CRUD (도메인별 필터) |
| `SchemaManagement` | 커스텀 스키마 등록 및 조회 |
| `ResourceTypeManagement` | SCIM ResourceType 관리 |
| `SyncHistory` | 동기화 이벤트 장부 |

## 디렉터리 구조

```
src/
├── api/            (client.ts, UserService.ts, HistoryService.ts)
├── components/
│   ├── common/     (StatusBadge, OperationBadge, UserProfileViewer)
│   ├── layout/     (AppSidebar)
│   ├── sync/       (HistoryTable, SyncPipeline, RawPayloadViewer)
│   ├── attribute/  (AttributeForm)
│   └── ui/         (Shadcn Vue — Button, Badge, Sheet, Breadcrumb …)
├── composables/    (useContainerWidth)
├── stores/         (miller.ts, attribute.ts, resourceType.ts)
├── types/          (scim.ts, attribute.ts, index.ts)
├── utils/          (theme.ts, date.ts, scim-metadata.ts, toast.ts)
└── views/          (OrgUserManagement, AttributeManagement, …)
```

## 디자인 원칙

- **밀도 우선:** base font 13px, `h-8 / px-2`, border-radius ≤ 4px, Neutral 팔레트
- **테마:** Source=Blue, Integration=Purple, Audit=Amber, IAM=Orange (`utils/theme.ts`)
- Options API 금지 — `<script setup lang="ts">` 필수
- 빌드 검증(`pnpm run build`) 통과 후 브라우저 확인

## 참고

- 전체 아키텍처: [`../spec.md`](../spec.md)
- Claude Code 편집 규칙: [`CLAUDE.md`](./CLAUDE.md)
