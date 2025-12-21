<script setup lang="ts">
import { useMillerStore } from './stores/miller'
import { onMounted } from 'vue'

const millerStore = useMillerStore()

onMounted(() => {
  // Initial pane
  millerStore.pushPane({
    id: 'user-list-main',
    type: 'UserList',
    title: 'Account Explorer',
    data: {}
  })
})
</script>

<template>
  <div class="iam-container">
    <!-- Header -->
    <header class="h-10 border-b border-neutral-200 bg-white flex items-center px-4 shrink-0">
      <h1 class="text-sm font-semibold text-neutral-800">IAM Manager</h1>
    </header>

    <!-- Main Miller Column Area -->
    <main class="flex-1 flex overflow-x-auto overflow-y-hidden">
      <div 
        v-for="pane in millerStore.panes" 
        :key="pane.id"
        class="iam-pane"
        :style="{ minWidth: pane.width || '450px' }"
      >
        <!-- Pane Header -->
        <div class="h-9 border-b border-neutral-100 bg-neutral-50/50 flex items-center px-3 justify-between shrink-0">
          <span class="text-[12px] font-medium text-neutral-600 uppercase tracking-tight">{{ pane.title }}</span>
          <div class="flex gap-1">
            <button class="iam-btn-xs bg-white border border-neutral-200 hover:bg-neutral-50">Filter</button>
          </div>
        </div>

        <!-- Pane Content (Placeholder) -->
        <div class="flex-1 overflow-y-auto p-2">
          <div v-if="pane.type === 'UserList'" class="space-y-1">
            <div 
              v-for="i in 20" 
              :key="i"
              @click="millerStore.pushPane({ id: 'user-' + i, type: 'UserDetail', title: 'User: iam_user_' + i, data: { userId: i } })"
              class="iam-table-cell hover:bg-blue-50 cursor-pointer flex items-center justify-between group"
            >
              <span>iam_user_{{ i }}</span>
              <span class="text-[10px] text-neutral-400 group-hover:text-blue-500">DETAILS →</span>
            </div>
          </div>
          <div v-else class="p-4 text-center text-neutral-400">
             Content for {{ pane.type }} (ID: {{ pane.data.userId }})
          </div>
        </div>
      </div>
    </main>

    <!-- Footer / Status Bar -->
    <footer class="h-6 border-t border-neutral-200 bg-white flex items-center px-3 shrink-0 text-[10px] text-neutral-500">
      <span>READY</span>
      <span class="mx-2">|</span>
      <span>API: Connected (v1.0.0)</span>
    </footer>
  </div>
</template>

<style>
/* Reset some default vite styles if needed */
#app {
  height: 100vh;
  width: 100vw;
}
</style>
