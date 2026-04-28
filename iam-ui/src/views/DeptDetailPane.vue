<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Network, Lock, Users as UsersIcon, ChevronRight } from 'lucide-vue-next'
import { useMillerStore } from '@/stores/miller'
import { useAttributeStore } from '@/stores/attribute'
import { DepartmentService } from '@/api/DepartmentService'
import type { Department } from '@/types'

const props = defineProps<{
    deptId: string
    paneIndex?: number
}>()

const millerStore = useMillerStore()
const attrStore = useAttributeStore()

const dept = ref<Department | null>(null)
const loading = ref(false)

onMounted(async () => {
    loading.value = true
    try {
        const [deptData] = await Promise.all([
            DepartmentService.getDepartment(props.deptId),
            attrStore.deptAttributes.length === 0 ? attrStore.fetchAttributes() : Promise.resolve()
        ])
        dept.value = deptData ?? null
    } finally {
        loading.value = false
    }
})

// Root-level dept attributes in schema order, sub-attrs excluded
const deptAttrs = computed(() =>
    attrStore.deptAttributes.filter(a => !a.parentName)
)

// Pair each attr meta with the live value from dept data
const rows = computed(() =>
    deptAttrs.value.map(meta => ({
        meta,
        value: dept.value?.[meta.name] ?? null
    }))
)

function formatValue(value: any): string {
    if (value === null || value === undefined || value === '') return '—'
    if (typeof value === 'boolean') return value ? 'Active' : 'Inactive'
    return String(value)
}

function openMembers() {
    if (!dept.value) return
    millerStore.setPane((props.paneIndex ?? 0) + 1, {
        id: `dept-users-${dept.value.id}`,
        type: 'OrgUserManagement',
        title: `${dept.value.displayName} Members`,
        data: { initialDeptId: dept.value.id },
        width: '800px',
        maxWidth: '800px'
    })
}
</script>

<template>
    <div class="h-full flex flex-col bg-white overflow-hidden">
        <!-- Loading -->
        <div v-if="loading" class="flex-1 flex items-center justify-center">
            <div class="size-5 border-2 border-blue-600/20 border-t-blue-600 rounded-full animate-spin"></div>
        </div>

        <template v-else-if="dept">
            <!-- Header -->
            <div class="p-4 border-b border-neutral-100 bg-neutral-50/30 shrink-0">
                <div class="flex items-start justify-between">
                    <div class="flex items-center gap-3">
                        <div class="size-9 bg-white border border-neutral-200 rounded-lg flex items-center justify-center text-blue-600 shadow-sm">
                            <Network class="size-4" />
                        </div>
                        <div>
                            <h2 class="text-sm font-black text-neutral-900 leading-tight uppercase tracking-tight">
                                {{ dept.displayName }}
                            </h2>
                            <div class="flex items-center gap-2 mt-0.5">
                                <span class="text-[9px] text-neutral-400 font-mono">{{ dept.id }}</span>
                                <span v-if="dept.externalId" class="text-[9px] text-blue-400 font-mono bg-blue-50 px-1 rounded">
                                    EXT: {{ dept.externalId }}
                                </span>
                            </div>
                        </div>
                    </div>
                    <span
                        class="text-[9px] font-bold px-2 py-0.5 rounded border"
                        :class="dept.active ? 'bg-green-50 text-green-600 border-green-200' : 'bg-neutral-100 text-neutral-400 border-neutral-200'"
                    >
                        {{ dept.active ? 'ACTIVE' : 'INACTIVE' }}
                    </span>
                </div>
            </div>

            <!-- Attributes toolbar -->
            <div class="h-9 border-b border-neutral-100 flex items-center px-3 shrink-0 bg-neutral-50/30" data-testid="dept-attr-toolbar">
                <span class="text-[10px] font-bold text-neutral-500 uppercase tracking-wider">
                    Attributes
                    <span class="ml-1 text-neutral-400 font-normal" data-testid="dept-attr-count">({{ deptAttrs.length }})</span>
                </span>
            </div>

            <!-- Attribute rows -->
            <div class="flex-1 overflow-y-auto custom-scrollbar">
                <div class="divide-y divide-neutral-50">
                    <div
                        v-for="row in rows"
                        :key="row.meta.name"
                        class="flex items-center px-3 py-2 hover:bg-neutral-50/60 transition-colors"
                    >
                        <!-- Label -->
                        <div class="w-36 shrink-0 flex items-center gap-1.5">
                            <span class="text-[10px] font-bold text-neutral-400 uppercase tracking-tighter truncate">
                                {{ row.meta.displayName }}
                            </span>
                            <Lock
                                v-if="row.meta.mutability === 'READ_ONLY' || row.meta.mutability === 'IMMUTABLE'"
                                class="size-2.5 text-neutral-300 shrink-0"
                            />
                        </div>

                        <!-- Value -->
                        <div class="flex-1 min-w-0">
                            <span
                                class="text-[11px] font-medium truncate block"
                                :class="row.value !== null && row.value !== '' ? 'text-neutral-800' : 'text-neutral-300 italic'"
                            >
                                {{ formatValue(row.value) }}
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Actions -->
            <div class="shrink-0 p-3 border-t border-neutral-100 bg-neutral-50/30">
                <button
                    @click="openMembers"
                    class="w-full py-1.5 bg-neutral-900 text-white rounded text-[10px] font-bold hover:bg-neutral-800 transition-colors flex items-center justify-center gap-2"
                >
                    <UsersIcon class="size-3" /> VIEW MEMBERS
                    <ChevronRight class="size-3 ml-auto" />
                </button>
            </div>
        </template>

        <!-- Not found -->
        <div v-else class="flex-1 flex items-center justify-center text-[11px] text-neutral-300 italic">
            Department not found
        </div>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #f1f1f1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #e5e5e5; }
</style>
