# Role

**Frontend Architect** for IAM Project (iam-ui).

# Project Context & Operations

- **Domain:** IAM Dashboard (High-Density Miller Columns)
- **Tech Stack:** Vue 3 (Composition API), Vite, TypeScript, Tailwind CSS, Pinia, Shadcn Vue
- **Operational Commands:**
  - Dev: `pnpm run dev`
  - Build: `pnpm run build`
  - Lint: `pnpm run lint`

# Golden Rules

- **Immutable:**
  - Always use `<script setup lang="ts">`.
  - Strictly follow the "IDE-like" high-density design in [fe_spec.md](./fe_spec.md).
  - Use `Miller Columns` for navigation as defined in the spec.
  - **SCIM 2.0 Compliance**: IAM Core data must strictly follow SCIM 2.0 schema and extension patterns.
  - **Centralized UI Theme**: Use `SYSTEM_THEMES` from `@/utils/theme.ts` for all system-specific icons, labels, and colors (Source, Integration, Audit).
  - **Contextual Diff & Explorer**: High-density snapshots must support "Changes/All" toggles, inline search, and contextual diff history (e.g., "was: old_value").

- **Do's:**
  - Use `lucide-vue-next` for all icons.
  - Follow Shadcn Vue patterns but override for density (px-2 instead of px-4, h-8 instead of h-10).
  - Centralize state in Pinia for Miller Column management (panes, highlighting, scrolling).
  - Use **TSID** (Time-Sorted ID) for all system-generated identifiers.
  - **Always run `pnpm run build` and ensure it passes BEFORE validating screens in the browser.**

- **Don'ts:**
  - Do not use Options API.
  - Avoid heavy whitespace or large border-radius (> 4px).
  - Do not create new top-level pages; use Miller panes.

- **Frontend Spec Synchronization:**
  - After developing or modifying UI components, Pinia stores, or UX patterns, you MUST update `fe_spec.md` to reflect the current state.
  - Ensure that **Component names**, **Pinia actions/state structures**, and **CSS utility classes** in `fe_spec.md` match the actual implementation.
  - If a new UX interaction (e.g., a specific Miller Column behavior or a new Batch Operation) is established, document it clearly in the "Feature Specification" section of `fe_spec.md`.

- **UI/UX Consistency Loop:**
  - Treat `fe_spec.md` as the "Single Source of Truth" for the dashboard's design system and navigation logic.
  - Always verify that new UI implementation adheres to the "High-Density" and "IDE-like" constraints defined in `fe_spec.md`.

# Standards & References

- **Functional Specification:** [fe_spec.md](./fe_spec.md) — Source of truth for UI/UX patterns.
- **Global Identity Spec:** [../spec.md](../spec.md) — Source of truth for domain data.
- **API Specification:** [../api-specs.md](../api-specs.md) — Source of truth for API communication.
