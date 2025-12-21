---
trigger: glob
globs: *.vue
---

# Vue 3 + TypeScript Project Style Guide

This document outlines the coding standards and best practices for our Vue 3 application. It relies heavily on the **[Official Vue.js Style Guide](https://vuejs.org/style-guide/)** but is tailored for the **Composition API (`<script setup>`)** and **TypeScript**.

---

## 1. Naming Conventions

### 1.1 Component Names (Multi-word)

Component names should always be multi-word, except for root `App` components. This prevents conflicts with existing and future HTML elements.

* ❌ **Bad:** `Todo.vue`, `Handler.vue`
* ✅ **Good:** `TodoItem.vue`, `EventHandler.vue`

### 1.2 Single-File Component Names (PascalCase)

Filenames for `.vue` components should always use PascalCase.

* ❌ **Bad:** `userList.vue`, `user-list.vue`
* ✅ **Good:** `UserList.vue`

### 1.3 Prop Names

* **Declaration (Script):** Use `camelCase`.
* **Template (HTML):** Use `kebab-case`.

```vue
<template>
  <UserProfile user-id="123" />
</template>
```

```ts
// UserProfile.vue
// ✅ Use camelCase in props definition
defineProps<{
  userId: string
}>()
```

---

## 2. Component Structure

### 2.1 Single-File Component Top-Level Element Order

To maintain consistency across the project, enforce the following order for top-level tags:

1. `<script setup>`
2. `<template>`
3. `<style>`

```vue
<script setup lang="ts">
/* Logic */
</script>

<template>
</template>

<style scoped>
/* CSS */
</style>
```

### 2.2 `<script setup>` Internal Order

Organize the code inside the script tag logically:

1. **Imports** (Vue core -> Third-party -> Local Components -> Types/Utils)
2. **Props & Emits** definitions
3. **Reactive State** (`ref`, `reactive`, `computed`)
4. **Lifecycle Hooks** (`onMounted`, `watch`, etc.)
5. **Methods / Event Handlers**

---

## 3. Template Rules (Priority A - Essential)

### 3.1 Keyed `v-for`

Always use `key` with `v-for`. The key must be unique and primitive (string or number), not an object.

* ❌ **Bad:** `<div v-for="item in items"></div>`
* ✅ **Good:** `<div v-for="item in items" :key="item.id"></div>`

### 3.2 Avoid `v-if` with `v-for`

Never use `v-if` on the same element as `v-for`.

* **Reason:** In Vue 3, `v-if` has higher priority than `v-for`, so the `v-if` condition won't have access to the variable from the `v-for` scope.
* **Solution:** Use a `computed` property to filter the list before iterating, or wrap the `v-for` in a `<template>`.

```vue
<ul>
  <li v-for="user in users" v-if="user.isActive" :key="user.id">
    {{ user.name }}
  </li>
</ul>

<ul>
  <li v-for="user in activeUsers" :key="user.id">
    {{ user.name }}
  </li>
</ul>
```

### 3.3 Component Style Scoping

Styles in App components should be `scoped` to prevent side effects, unless you are defining global base styles (which should usually be in `src/assets`).

* ✅ **Good:** `<style scoped>`

---

## 4. Template Rules (Priority B - Strongly Recommended)

### 4.1 Directive Shorthands

Always use shorthands. Do not mix full syntax and shorthands.

* `v-bind:href` -> `:href`
* `v-on:click` -> `@click`
* `v-slot:header` -> `#header`

```html
<input v-bind:value="newValue" v-on:input="onInput">

<input :value="newValue" @input="onInput">
```

### 4.2 Self-Closing Components

Components with no content should be self-closing in Single-File Components.

* ❌ **Bad:** `<MyComponent></MyComponent>`
* ✅ **Good:** `<MyComponent />`

### 4.3 Simple Expressions in Templates

Keep template expressions simple. If logic becomes complex, extract it into a `computed` property or a method.

```vue
{{ fullName.split(' ').map((word) => word[0].toUpperCase() + word.slice(1)).join(' ') }}

{{ normalizedFullName }}
```

---

## 5. TypeScript & Composition API Best Practices

### 5.1 Type-Only Props/Emits

Use TypeScript generics for `defineProps` and `defineEmits` for better type inference.

```ts
// ❌ Runtime Declaration (Less Type Safety)
const props = defineProps({
  name: String,
  age: Number
})

// ✅ Type-based Declaration
interface Props {
  name: string
  age?: number // Optional
}
const props = defineProps<Props>()
```

### 5.2 No Destructuring of Props

Do not destructure the `props` object, as it will lose reactivity. If you need a reactive variable from props, use `toRef` or `toRefs` (or access via `props.propertyName`).

```ts
const props = defineProps<Props>()

// ❌ Bad (Loses reactivity)
const { name } = props

// ✅ Good
import { toRefs } from 'vue'
const { name } = toRefs(props)
```

### 5.3 Avoid `any`

Avoid using `any` whenever possible. Define interfaces or types for your data models.

---

## 6. Directory Structure

A recommended structure to keep the project organized:

```
src/
├── assets/          # Static assets (images, global css)
├── components/      # Shared/Global components
│   ├── common/      # Generic UI (Button, Input, Modal)
│   └── layout/      # Header, Sidebar, Footer
├── composables/     # Shared logic (Vue hooks)
├── router/          # Vue Router configuration
├── stores/          # Pinia stores
├── types/           # Global TypeScript interfaces
├── views/           # Page components (Routed pages)
├── utils/           # Helper functions
├── App.vue
└── main.ts
```
