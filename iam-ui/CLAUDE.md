# iam-ui — Claude Code 컨텍스트

**역할:** Vue 3 + TypeScript + Vite 프론트엔드. IAM Registry 의 SCIM 데이터를 고밀도 "IDE-like" Miller Columns UI 로 시각화.

- 루트 컨텍스트: [../CLAUDE.md](../CLAUDE.md)
- DynamicUserForm 패턴: [../.agent/rules/dynamic-form-guide.md](../.agent/rules/dynamic-form-guide.md)
- Vue 코드 스타일: [../.agent/rules/vue-code-style-guide.md](../.agent/rules/vue-code-style-guide.md)

## 핵심 제약

- **패키지 매니저: `pnpm`** — npm/yarn 혼용 금지
- **`<script setup lang="ts">` 필수** — Options API 금지
- **빌드 검증:** 브라우저 확인 전 반드시 `pnpm run build` 통과
- **페이지 라우팅 대신 Miller Columns** — `stores/miller.ts` 의 `useMillerStore`
- **아이콘:** `lucide-vue-next` / **테마:** `@/utils/theme.ts` 의 `SYSTEM_THEMES`
- **밀도 우선:** radius ≤ 4px, base font 13px, Neutral 팔레트, `h-8 / px-2` 톤
