# Miller Columns 패턴 가이드

> 관련: `iam-ui/src/stores/miller.ts`, `iam-ui/src/App.vue`, `iam-ui/src/composables/useMillerSizes.ts`

## 개념

Miller Columns는 계층형 데이터를 **좌→우 컬럼 스택**으로 탐색하는 UX 패턴입니다.  
macOS Finder의 열 보기 모드와 같은 방식으로, 항목을 선택하면 하위 상세 패널이 오른쪽에 열립니다.

이 프로젝트에서는 페이지 라우팅 없이 `useMillerStore`의 `panes[]` 배열로 패널 스택을 관리합니다.

```
[사이드바] → [LVL.1: Users]  →  [LVL.2: User Detail]  →  [LVL.3: Sync History]
                W3 패널               W1 패널                    W2 패널
```

---

## 패널 너비 시스템

### 너비 토큰

하드코딩된 픽셀값 대신 `'w1'` / `'w2'` / `'w3'` 토큰을 사용합니다.  
`App.vue`의 `resolveWidth()` 함수가 런타임에 픽셀값으로 변환합니다.

| 토큰 | 단위 | 용도 예시 |
|---|---|---|
| `'w1'` | 1단위 | 상세 패널, 폼, 스키마, 속성 등 대부분의 패널 |
| `'w2'` | 2단위 | 이력 목록 (SyncHistory, UserChangeHistory) |
| `'w3'` | 3단위 | 주요 데이터 테이블 (OrgUserManagement) |

### 너비 계산 원리

`useMillerSizes.ts`의 ResizeObserver가 스크롤 컨테이너를 감시합니다.

```
W1 = floor((containerWidth - 24px_peek - 2×8px_gap) / 3)
W2 = W1 × 2 + 8
W3 = W1 × 3 + 16
```

- **peek(24px)**: LVL.N+1 패널이 열려 있음을 암시하는 슬라이버
- **gap(8px)**: 패널 간 간격 (`gap-2`)
- **사이드바 접힘 자동 반응**: 스크롤 컨테이너가 `SidebarInset` 안에 있어 사이드바 토글 시 너비가 변하면 ResizeObserver가 즉시 W1~W3을 재계산

---

## 패널 등록 방법

### 1. 새 패널 열기

```ts
import { useMillerStore } from '@/stores/miller'

const millerStore = useMillerStore()

millerStore.setPane(props.paneIndex + 1, {
    id: `myview-${someId}`,     // 고유 ID (중복 방지)
    type: 'MyViewComponent',    // VIEW_COMPONENTS 키 (App.vue에 등록 필요)
    title: '패널 제목',
    data: { /* props로 전달될 데이터 */ },
    width: 'w1'                 // 'w1' | 'w2' | 'w3'
})
```

### 2. App.vue에 컴포넌트 등록

```ts
// App.vue VIEW_COMPONENTS
const VIEW_COMPONENTS: Record<string, any> = {
    MyViewComponent,       // type 문자열 → import한 컴포넌트
    ...
}
```

### 3. 패널 컴포넌트 시그니처

모든 패널 컴포넌트는 `paneIndex` prop을 받아야 하위 패널을 열 수 있습니다.

```ts
const props = defineProps<{
    paneIndex?: number
    // ... 기타 data props
}>()
```

---

## 주요 Store 메서드

```ts
millerStore.pushPane(pane)            // 스택 끝에 패널 추가
millerStore.setPane(index, pane)      // 특정 인덱스에 패널 교체 (우측 패널 초기화 포함)
millerStore.removePane(index)         // 패널 제거 (우측 패널도 함께 제거)
millerStore.activePaneId = id         // 활성 패널 포커스 (강조 링 표시)
millerStore.highlightPane(id)         // 기존 패널 하이라이트 (새로 열지 않고)
```

---

## 사이드바 네비게이션 너비 규칙

`AppSidebar.vue`에서 최초 패널을 열 때 아래 규칙을 따릅니다.

```ts
width: item.view === 'OrgUserManagement' ? 'w3'
     : (item.view === 'SyncHistory' || item.view === 'UserChangeHistory') ? 'w2'
     : 'w1'
```

새 뷰를 추가할 때 이 조건에 맞춰 너비를 지정하거나 기본값 `'w1'`을 유지하세요.

---

## 패널 중복 방지 패턴

같은 항목을 두 번 클릭해도 패널이 중복 생성되지 않도록 항상 ID를 확인합니다.

```ts
const paneId = `myview-${item.id}`
const existing = millerStore.panes.find(p => p.id === paneId)
if (existing) {
    millerStore.activePaneId = paneId
    return
}
millerStore.setPane(props.paneIndex + 1, { id: paneId, ... })
```

---

## 자주 하는 실수

| 실수 | 올바른 방법 |
|---|---|
| `width: '500px'` 하드코딩 | `width: 'w1'` 토큰 사용 |
| `maxWidth` 별도 지정 | `App.vue`가 `width`로 `maxWidth`도 함께 처리 |
| `pushPane`으로 상세 패널 열기 | `setPane(paneIndex + 1, ...)` 사용 — 우측 불필요 패널이 자동 제거됨 |
| `paneIndex` prop 없는 컴포넌트 | 하위 패널을 열 수 없으므로 반드시 선언 |
