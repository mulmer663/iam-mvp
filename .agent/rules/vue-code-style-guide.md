---
trigger: glob
globs: *.vue
---

# Vue 3 + TypeScript Project Style Guide

## 1. Naming & Structure

* **Component Names:** Use multi-word names (e.g., `TodoItem.vue`). Use **PascalCase** for filenames.
* **Prop Names:** Use `camelCase` in scripts and `kebab-case` in templates.
* **Tag Order:** Follow the order: 1. `<script setup>`, 2. `<template>`, 3. `<style scoped>`.
* **Script Internal Order:** Imports → Props/Emits → Reactive State → Lifecycle → Methods.

## 2. Template & Logic

* **Directives:** Always use shorthands (`:href`, `@click`, `#header`). Use self-closing tags for empty components.
* **Lists:** Always use a unique primitive `key` with `v-for`. Never use `v-if` on the same element as `v-for`.
* **Reactivity:** Use generic types for `defineProps<Props>()`. Do **not** destructure `props` to maintain reactivity; use `toRefs` if needed.

## 3. Directory Structure

* A recommended structure to keep the project organized:

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
