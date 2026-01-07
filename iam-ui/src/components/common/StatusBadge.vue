<script setup lang="ts">
import { Badge } from '@/components/ui/badge'
import { computed } from 'vue'

const props = defineProps<{
  status: string
}>()

const style = computed(() => {
  switch (props.status.toUpperCase()) {
    case 'SUCCESS': 
      return { variant: 'outline' as const, class: 'border-emerald-500 text-emerald-600' }
    case 'FAILURE': 
      return { variant: 'outline' as const, class: 'border-red-500 text-red-600' }
    case 'PENDING': 
      return { variant: 'outline' as const, class: 'border-amber-500 text-amber-600' }
    default: 
      return { variant: 'outline' as const, class: 'border-neutral-300 text-neutral-500' }
  }
})
</script>

<template>
  <Badge 
    :variant="style.variant" 
    class="px-1 py-0 h-4 text-[9px] font-bold rounded-sm uppercase"
    :class="style.class"
  >
    <slot>{{ status }}</slot>
  </Badge>
</template>
