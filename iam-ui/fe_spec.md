# Project: High-Density IAM Dashboard (Vue 3 + Shadcn)

## 1. Project Overview

- **Goal**: Build a specialized Identity Access Management (IAM) dashboard focused on information density, continuous exploration (no page reloads), and secure batch operations.
- **Design Philosophy**: "IDE-like" interface. High density, minimal whitespace, neutral colors, and keyboard-centric navigation.
- **Core UX Patterns**:
  1. **Miller Columns**: Horizontal drill-down navigation (Mac Finder style).
  2. **Batch Operations**: Excel-like bulk editing with floating action bars.

## 2. Tech Stack & Environment

- **Framework**: Vue 3 (Composition API, `<script setup lang="ts">`)
- **Build Tool**: Vite
- **UI System**: Shadcn Vue (Radix Vue based)
  - **Preset**: `style=nova`, `baseColor=neutral`, `radius=small`
  - **Icons**: HugeIcons (via `hugeicons-vue` or similar)
- **State Management**: Pinia
- **CSS Engine**: Tailwind CSS
- **Package Manager**: pnpm

## 3. Design Tokens & Global Styles

**Constraint**: The UI must be significantly denser than the default Shadcn theme.

### 3.1 Global CSS Overrides (`globals.css`)

- **Radius**: Force `0.25rem` (4px) or smaller.
- **Font Size**: Base font size `0.8125rem` (13px) for data tables.
- **Colors**: Strictly `Neutral` palette. Use color only for status (Green=Active, Red=Danger/Deleted, Amber=Warning).

### 3.2 Utility Classes (Apply these patterns)

- `.iam-container`: `h-screen w-screen overflow-hidden bg-neutral-50 flex flex-col`
- `.iam-pane`: `min-w-[400px] max-w-[600px] border-r border-neutral-200 bg-white h-full flex flex-col`
- `.iam-table-cell`: `p-2 h-8 text-xs align-middle border-b border-neutral-100`
- `.iam-btn-xs`: `h-6 px-2 text-[11px] rounded-sm`
- `.iam-input`: `h-8 text-xs`

---

## 4. Feature Specification: Miller Columns (Infinite Explorer)

### 4.1 Concept

Instead of routing (`vue-router` page transitions), use a dynamic stack of panels.

- **Level 1**: User List
- **Level 2**: User Details (clicked from L1)
- **Level 3**: Role List (clicked from L2)
- **Level 4**: Policy Editor (clicked from L3)

### 4.2 Data Structure (Pinia Store: `useMillerStore`)

```typescript
interface Pane {
  id: string;          // unique ID (e.g., 'user-list-main', 'user-detail-101')
  type: string;        // component type key (e.g., 'UserList', 'UserDetail', 'RoleGraph')
  title: string;       // Header title
  data: any;           // Props or Context to pass to the component
  width?: string;      // Custom width (default: 450px)
}

state: {
  panes: Pane[];       // Array of active panes
  activePaneId: string;
}

actions: {
  pushPane(pane: Pane); // Adds a new pane to the right. Removes any existing forward history.
  popToPane(paneId: string); // Closes all panes to the right of target.
  replacePane(index: number, pane: Pane); // Replaces the pane at index and removes all subsequent panes (Singleton Interaction).
  updatePaneData(paneId: string, newData: any);
}
```

### 4.3 Interaction Rules

1. **Singleton Detail**: When clicking an item in a list (e.g., a specific User), do not keep stacking details. Replace the existing Detail pane at that level.
   - Example: `[List] -> [User A]` --> Click User B --> `[List] -> [User B]` (replaces User A).
2. **Dynamic Flex Layout**:
   - The **last (active)** pane should expand to fill the remaining screen space (`flex-1`).
   - Previous panes should shrink to their minimum/default width (`min-w-[450px]`).
   - This ensures focus remains on the current task while maintaining context.
