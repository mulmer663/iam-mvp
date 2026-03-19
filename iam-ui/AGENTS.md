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
  - Strictly follow the "IDE-like" high-density design specified below.
  - Use `Miller Columns` for navigation.
  - **SCIM 2.0 Compliance**: IAM Core data must strictly follow SCIM 2.0 schema and extension patterns.
  - **Centralized UI Theme**: Use `SYSTEM_THEMES` from `@/utils/theme.ts` for all system-specific icons, labels, and colors (Source, Integration, Audit).
  - **Contextual Diff & Explorer**: High-density snapshots must support "Changes/All" toggles, inline search, and contextual diff history.

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

# Architecture & Design Specifications

- **Goal**: Build a specialized Identity Access Management (IAM) dashboard focused on information density, continuous exploration (no page reloads), and secure batch operations.
- **Design Philosophy**: "IDE-like" interface. High density, minimal whitespace, neutral colors, and keyboard-centric navigation.

### Global CSS Overrides (`globals.css`)

- **Radius**: Force `0.25rem` (4px) or smaller.
- **Font Size**: Base font size `0.8125rem` (13px) for data tables.
- **Colors**: Strictly `Neutral` palette. Use color only for status (Green=Active, Red=Danger/Deleted, Amber=Warning).

### Utility Classes

- `.iam-container`: `h-screen w-screen overflow-hidden bg-neutral-50 flex flex-col`
- `.iam-pane`: `min-w-[400px] max-w-[600px] border-r border-neutral-200 bg-white h-full flex flex-col`
- `.iam-table-cell`: `p-2 h-8 text-xs align-middle border-b border-neutral-100`

### Miller Columns (Infinite Explorer) Concept

Instead of routing (`vue-router` page transitions), use a dynamic stack of panels.

- **Level 1**: Org & User Management (`OrgUserManagement.vue`)
- **Level 2**: User Details (`UserDetail` view)
- **Level 3**: Exploratory History/Logs (`SyncHistory.vue`) (Source/Integration Sync History)
- **Level 4**: Detailed Event Analysis (`SyncDetail.vue`) (Pipeline tracing, Snapshot diffing)

### Data Structure (Pinia Store: `useMillerStore`)

```typescript
interface MillerPane {
  id: string;          // unique ID (e.g., 'user-list-main', 'sync-detail-101')
  type: string;        // component type key
  title: string;       // Header title
  data: any;           // Props or Context to pass
  width?: string;      // Custom width (e.g., '450px', '800px')
  maxWidth?: string;   // Constraint for auto-expansion
}
```

### Advanced Interactions

1. **Smart Redirection**: Clicking a link to an entity already visible triggers `@keyframes flash` (Blue glow) on the existing pane and scrolls to it.
2. **Smooth Transitions**: `TransitionGroup` with `v-move` ensures panes slide smoothly.

### Centralized UI Theme System

- **SOURCE (Blue)**: HR Sync events. Icon: `Database`.
- **INTEGRATION (Purple)**: AD/Target System provisioning. Icon: `Server`.
- **AUDIT (Amber)**: IAM Core modification ledger. Icon: `History`.
- **IAM (Orange)**: Identity management core state. Icon: `Activity`.

### Search & Navigation Structure

- **Search**: `Ctrl+K` global search placeholder.
- **Breadcrumbs**: Flat level-based navigation in the header (`Dashboard > Users & Org`).
- **Quick Close**: Keyboard ESC label on panes to trigger `popPane`.

# Context Map

- **[Master AGENTS.md](../AGENTS.md)** — Root project governance and navigation.
- **[Global IAM Spec](../spec.md)** — Source of truth for domain data and backend API design.
