# Playwright E2E 테스트 가이드

## 실행 명령

```bash
cd iam-ui
pnpm test:e2e          # 헤드리스 실행 (CI / Claude 검증용)
pnpm test:e2e:ui       # UI 모드 (사람이 직접 볼 때)
pnpm exec playwright test e2e/foo.spec.ts  # 단일 파일만
```

> **전제:** `pnpm run dev` + `./gradlew :iam-registry:bootRun` 실행 중이어야 함

---

## Claude가 테스트를 사용하는 방법

### 버그 수정 후 검증 플로우
1. 수정 완료
2. `pnpm test:e2e` 실행
3. 결과 확인 → 통과하면 완료 보고, 실패하면 원인 분석 후 재수정

### 토큰 절약 원칙
- 스크린샷은 `only-on-failure` — 통과 시 불필요한 이미지 없음
- `waitForLoadState('networkidle')` 대신 특정 요소 wait 우선 (`waitForSelector`)
- 테스트 하나당 검증 포인트를 3개 이하로 유지
- 실패 시 error message만 읽고 판단 — 전체 스크린샷 열람은 최후 수단

---

## 테스트 작성 규칙

### 파일 구조
```
e2e/
  schema-detail-pane.spec.ts   # 뷰/컴포넌트 단위로 파일 분리
  dept-management.spec.ts
  user-table.spec.ts
```

### 셀렉터 우선순위
1. `getByRole` (접근성 기반, 가장 안정적)
2. `getByText` (텍스트 매칭)
3. `locator('[data-testid="..."]')` (명시적 testid, 필요 시 추가)
4. CSS class 셀렉터는 **금지** — Tailwind 클래스는 리팩터링에 취약

### 비동기 처리
```ts
// Good: 특정 요소가 나타날 때까지
await expect(page.getByText('Attributes')).toBeVisible()

// Bad: 고정 시간 대기
await page.waitForTimeout(2000)
```

### API 응답 대기
```ts
// 네트워크 응답까지 기다려야 할 때
await page.waitForResponse(resp => resp.url().includes('/scim/v2/'))
```

---

## 테스트 대상 우선순위

| 우선순위 | 대상 | 이유 |
|---|---|---|
| P0 | SchemaDetailPane 도메인 매핑 | 버그 재발 방지 |
| P0 | DeptManagement 실데이터 로드 | 목 → 실API 전환 |
| P1 | UserTable 필터링 (부서별) | 핵심 사용 흐름 |
| P1 | AttributeFormPane 저장/수정 | 데이터 무결성 |
| P2 | Miller Columns 패널 이동 | UX 회귀 방지 |

---

## 네비게이션 패턴 (앱 구조 기반)

앱은 Miller Columns 구조 — 사이드바 아이콘으로 섹션 전환.

```ts
// 스키마 관리 진입
await page.goto('/')
await page.locator('[title="Schema"]').click()  // 또는 실제 aria-label 확인 필요

// 특정 스키마 선택 후 패널 로드 대기
await page.getByText('urn:iam:params:scim').click()
await expect(page.getByText('Attributes')).toBeVisible()
```

실제 셀렉터는 첫 실행 시 `--headed` 모드로 확인:
```bash
pnpm exec playwright test --headed --slow-mo=500
```
