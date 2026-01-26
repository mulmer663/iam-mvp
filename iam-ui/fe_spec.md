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

- **Level 1**: Org & User Management (`OrgUserManagement.vue`)
- **Level 2**: User Details (`UserDetail` view)
- **Level 3**: Exploratory History/Logs (`SyncHistory.vue`)
  - *Source Sync History*, *Integration Sync History*, *Modification Ledger*
- **Level 4**: Detailed Event Analysis (`SyncDetail.vue`)
  - *Pipeline tracing*, *Snapshot diffing*, *Attribute mapping*

### 4.2 Data Structure (Pinia Store: `useMillerStore`)

```typescript
interface MillerPane {
  id: string;          // unique ID (e.g., 'user-list-main', 'sync-detail-101')
  type: string;        // component type key (e.g., 'OrgUserManagement', 'SyncDetail')
  title: string;       // Header title
  data: any;           // Props or Context to pass to the component
  width?: string;      // Custom width (e.g., '450px', '800px')
  maxWidth?: string;   // Constraint for auto-expansion
}

state: {
  panes: MillerPane[];       // Array of active panes
  activePaneId: string | null;
  highlightedPaneId: string | null; // ID for flash-highlight animation
}

actions: {
  pushPane(pane: MillerPane); // Adds a new pane to the right.
  popPane();                 // Removes the last pane.
  popToPane(id: string);     // Closes all panes to the right of target.
  setPane(index: number, pane: MillerPane); // Replaces the pane at index and removes subsequent.
  highlightPane(id: string); // Triggers visual flash + scroll to target.
}
```

### 4.3 Interaction Rules

1. **Singleton Detail**: When clicking an item in a list, replace the existing child pane instead of stacking.
   - Using `millerStore.setPane(parentIndex + 1, ...)` ensures only one branch is explored at a time.
2. **Dynamic Flex Layout**:
   - The **last** pane expands (`flex-1`) if no `maxWidth` is provided.
   - Non-active panes maintain their `width` (default `450px`).
   - Active pane ID tracking allows for "High-Density focus" (dimming non-active panes if needed).

### 4.4 Advanced Interaction: Pane Highlighting & Scrolling

1. **Smart Redirection**: When clicking a link to an entity already visible in the stack (e.g., a "Parent Sync Event" link):
   - **Highlight**: Triggers `@keyframes flash` (Blue glow) on the existing pane.
   - **Scroll**: Container automatically scrolls to center or bring the highlighted pane into view.
2. **Smooth Transitions**: `TransitionGroup` with `v-move` ensures panes slide smoothly when added or removed.

### 4.5 Centralized UI Theme System

Consistent color-coding across the platform using `SYSTEM_THEMES`:

- **SOURCE (Blue)**: HR Sync events. Icon: `Database`.
- **INTEGRATION (Purple)**: AD/Target System provisioning. Icon: `Server`.
- **AUDIT (Amber)**: IAM Core modification ledger. Icon: `History`.
- **IAM (Orange)**: Identity management core state. Icon: `Activity`.

### 4.6 Snapshot Explorer (High-Density Data UI)

For entities with many attributes (SCIM 2.0), snapshots implement:

1. **Contextual Diff**: Modified fields are highlighted using the theme's `container` style.
2. **View Mode Toggle**:
   - **CHANGES**: Show only modified attributes (detected via `event.changes`).
   - **ALL**: Show full snapshot with changed fields pinned/ranked to the top.
3. **Inline Search**: Real-time filtering within the snapshot attributes.
4. **Attribute Mapping**: Context-aware 2-way display (e.g., `HR Field -> IAM Field` or `IAM Field -> AD Field`).

### 4.7 Sync Pipeline (Event Tracing) [NEW]

The `SyncDetail` view implements a 3-step pipeline visualization using the backend's hierarchical logging:

1. **Hierarchy**: Use `parent_history_id` to link events (e.g., `RAW_INGEST` -> `TRANSFORM` -> `IAM_UPDATE`).
2. **Performance**: Display `duration_ms` for each stage to identify bottlenecks.
3. **Payloads**:
   - `message`: Use the human-readable outcome message.
   - `payload`: Use the flattened JSON (unwrapped `UniversalData`) for display.
4. **Source**: Links to the original HR event that triggered the change.
5. **Core**: The IAM internal modification record.
6. **Target**: Grid of downstream provisioning events (AD, GitHub, etc.) triggered by the same Trace ID.

Cross-pane navigation within the pipeline uses `highlightPane` if the target is already open.

### 4.8 Universal Search & Navigation [NEW]

- **Search**: `Ctrl+K` global search placeholder (Header).
- **Breadcrumbs**: Flat level-based navigation in the header (`Dashboard > Users & Org`).
- **Quick Close**: Keyboard ESC label on panes to trigger `popPane` (Contextual).

### 4.9 Attribute Management System [NEW]

A specialized UI for managing SCIM 2.0 Schemas and Attributes with high density and precision.

1. **Schema-Driven Architecture**:
   - **Root View**: Groups attributes by Schema URI (Core vs Extensions).
   - **Visual Distinction**:
     - **Core Schemas**: Blue Theme (`Database` icon). Fixed/System attributes.
     - **Extensions**: Purple Theme (`Settings` icon). Custom/Enterprise attributes.
2. **Detail View (`AttributeFormPane`)**:
   - **Mode-Aware**: Handles Create vs Edit vs View (Read-Only).
   - **Type Handling**: Dynamic form fields based on `type` (String, Boolean, Reference, etc.).
   - **Complex Types**:
     - **Inline Management**: Sub-attributes (e.g., `emails.value`, `emails.type`) are managed directly within the parent attribute's form.
     - **Embedded List**: A "Sub-Attributes" section appears for `COMPLEX` types, allowing rapid addition/editing without deep navigation stacks.
   - **Validation**: Enforces SCIM mutability rules (`readOnly`, `immutable`) and locks system fields.
3. **Component Mapping**:
   - `AttributeManagement` (List) -> `AttributeFormPane` (Detail/Edit).
