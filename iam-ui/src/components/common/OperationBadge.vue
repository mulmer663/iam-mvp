<script setup lang="ts">
import { Badge } from '@/components/ui/badge'
import { computed } from 'vue'

const props = defineProps<{
  type: string
}>()

const style = computed(() => {
  const t = props.type.toUpperCase()
  if (t.includes('CREATE') || t.includes('JOIN')) {
    return { variant: 'outline', class: 'border-blue-500 text-blue-600' }
  }
  if (t.includes('UPDATE')) {
    return { variant: 'outline', class: 'border-orange-500 text-orange-600' }
  }
  if (t.includes('DELETE') || t.includes('LEAVE')) {
    return { variant: 'outline', class: 'border-red-500 text-red-600' }
  }
  return { variant: 'outline', class: 'border-neutral-300 text-neutral-500' }
})
</script>

<template>
  <Badge 
    :variant="style.variant as any" 
    class="px-1 py-0 h-4 text-[9px] font-bold rounded-sm uppercase tracking-tighter"
    :class="style.class"
  >
    <slot>{{ type }}</slot>
  </Badge>
</template>
