---
trigger: glob
globs: "iam-ui/src/**/*.vue"
---

# DynamicUserForm 패턴 & UI 주의사항

## DynamicUserForm 위젯 결정 규칙

`IamAttributeMeta` 배열을 받아 UI를 자동 생성. 속성 이름 하드코딩 금지.

- `display === false` → 렌더링 제외
- `mutability === 'READ_ONLY'` / `'IMMUTABLE'` → 항상 비활성
- `mutability === 'WRITE_ONCE'` → 편집 모드에서만 비활성
- 위젯 결정 순서: `BOOLEAN` → Checkbox / `canonicalValues` 있음 → Select / `uiComponent === 'textarea'` → Textarea / 나머지 → Input
- 멀티밸류 COMPLEX (emails, phoneNumbers 등): flex-wrap 행, 셀 너비는 `type` / `canonicalValues` / `uiComponent` 기반
- `addresses.formatted` 는 `uiComponent='textarea'` 로 시드 — 전체 행 차지

## reka-ui Checkbox 바인딩

reka-ui v2.x 는 `:model-value` / `@update:model-value` 사용. `:checked` / `@update:checked` 는 동작 안 함.

```html
<Checkbox :model-value="formData.someFlag ?? true"
          @update:model-value="(v: any) => formData.someFlag = !!v" />
```
