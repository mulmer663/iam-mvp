<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Network, Save, X } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useMillerStore } from '@/stores/miller'
import { useAttributeStore } from '@/stores/attribute'
import { useDeptStore } from '@/stores/department'
import { toast } from '@/utils/toast'
import type { IamAttributeMeta } from '@/types/attribute'

const props = defineProps<{ paneIndex?: number }>()

const millerStore = useMillerStore()
const attrStore = useAttributeStore()
const deptStore = useDeptStore()

const saving = ref(false)
const formData = ref<Record<string, any>>({ active: true })

// All attrs except READ_ONLY (level, managerDisplayName are system-computed)
const createAttrs = computed(() =>
    attrStore.deptAttributes.filter(a => !a.parentName && a.mutability !== 'READ_ONLY')
)

// Existing depts for parentId dropdown
const deptOptions = computed(() =>
    deptStore.departments.map(d => ({ value: d.id, label: `${d.displayName} (${d.id})` }))
)

onMounted(async () => {
    await Promise.all([
        attrStore.deptAttributes.length === 0 ? attrStore.fetchAttributes() : Promise.resolve(),
        deptStore.departments.length === 0 ? deptStore.fetchAll() : Promise.resolve()
    ])
    initBlank()
})

function initBlank() {
    const blank: Record<string, any> = { active: true }
    for (const a of createAttrs.value) {
        if (a.type === 'BOOLEAN') blank[a.name] = false
        else if (a.type === 'INTEGER' || a.type === 'NUMBER') blank[a.name] = null
        else blank[a.name] = ''
    }
    blank.active = true
    formData.value = blank
}

function inputType(meta: IamAttributeMeta): string {
    if (meta.type === 'INTEGER' || meta.type === 'NUMBER') return 'number'
    return 'text'
}

function cleanPayload(obj: Record<string, any>): Record<string, any> {
    const out: Record<string, any> = {}
    for (const [k, v] of Object.entries(obj)) {
        if (v === '' || v === null || v === undefined) continue
        out[k] = v
    }
    return out
}

async function save() {
    if (!formData.value.id?.trim()) { toast.error('SCIM ID is required'); return }
    if (!formData.value.displayName?.trim()) { toast.error('Display Name is required'); return }
    saving.value = true
    try {
        const payload = cleanPayload(formData.value)
        const created = await deptStore.create(payload)
        toast.success(`Department created: ${created.displayName}`)
        millerStore.setPane(props.paneIndex ?? 1, {
            id: `dept-detail-${created.id}`,
            type: 'DeptDetailPane',
            title: created.displayName,
            data: { deptId: created.id, paneIndex: props.paneIndex ?? 1 }
        })
    } catch (e: any) {
        toast.error(e?.message || 'Failed to create department')
    } finally {
        saving.value = false
    }
}

function cancel() {
    millerStore.removePane(props.paneIndex ?? 1)
}
</script>

<template>
    <div class="h-full flex flex-col bg-white text-[12px]">
        <!-- Header -->
        <div class="border-b border-neutral-100 bg-neutral-50/60 px-4 py-3 shrink-0 flex items-center gap-2">
            <div class="size-7 rounded-md bg-blue-50 border border-blue-100 flex items-center justify-center">
                <Network class="size-3.5 text-blue-500" />
            </div>
            <div class="min-w-0">
                <div class="font-bold text-neutral-800 text-[13px]" data-testid="dept-create-title">New Department</div>
                <div class="text-[10px] text-neutral-400">Fields derived from Department Attribute Schema</div>
            </div>
        </div>

        <!-- Form -->
        <div class="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar">
            <!-- SCIM ID (infra field, not in attrMeta) -->
            <div>
                <label class="block text-[9px] font-bold text-neutral-400 uppercase tracking-tighter mb-0.5">
                    SCIM ID <span class="text-red-400">*</span>
                    <span class="text-neutral-300 ml-1 normal-case font-normal">— URL identifier (e.g. IT-SEC)</span>
                </label>
                <input v-model="formData.id" type="text" placeholder="e.g. IT-INFRA"
                    class="w-full h-7 px-2 text-[11px] bg-white border border-neutral-200 rounded focus:border-blue-400 focus:ring-1 focus:ring-blue-100 outline-none transition-all" />
            </div>

            <!-- parentId — dropdown -->
            <div>
                <label class="block text-[9px] font-bold text-neutral-400 uppercase tracking-tighter mb-0.5">
                    Parent Department
                </label>
                <select v-model="formData.parentId"
                    class="w-full h-7 px-2 text-[11px] bg-white border border-neutral-200 rounded focus:border-blue-400 outline-none transition-all">
                    <option value="">— None (root) —</option>
                    <option v-for="opt in deptOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
            </div>

            <!-- Schema-driven attrs (excluding parentId which is above) -->
            <template v-for="attr in createAttrs" :key="attr.name">
                <div v-if="attr.name !== 'parentId'">
                    <label class="block text-[9px] font-bold text-neutral-400 uppercase tracking-tighter mb-0.5">
                        {{ attr.displayName }}
                        <span v-if="attr.required" class="text-red-400 ml-0.5">*</span>
                    </label>
                    <!-- Boolean -->
                    <label v-if="attr.type === 'BOOLEAN'" class="flex items-center gap-2 cursor-pointer h-7">
                        <input type="checkbox" v-model="formData[attr.name]"
                            class="rounded border-neutral-300 text-blue-600" />
                        <span class="text-[11px] text-neutral-600">{{ formData[attr.name] ? 'Active' : 'Inactive' }}</span>
                    </label>
                    <!-- Textarea -->
                    <textarea v-else-if="attr.uiComponent === 'textarea'"
                        v-model="formData[attr.name]"
                        :placeholder="attr.description ?? attr.displayName"
                        rows="2"
                        class="w-full px-2 py-1.5 text-[11px] bg-white border border-neutral-200 rounded focus:border-blue-400 focus:ring-1 focus:ring-blue-100 outline-none transition-all resize-none"
                    />
                    <!-- Text / Number -->
                    <input v-else
                        :type="inputType(attr)"
                        v-model="formData[attr.name]"
                        :placeholder="attr.description ?? attr.displayName"
                        class="w-full h-7 px-2 text-[11px] bg-white border border-neutral-200 rounded focus:border-blue-400 focus:ring-1 focus:ring-blue-100 outline-none transition-all"
                    />
                </div>
            </template>
        </div>

        <!-- Footer -->
        <div class="border-t border-neutral-100 bg-neutral-50/40 px-3 py-2 flex items-center justify-end gap-2 shrink-0">
            <Button variant="ghost" size="xs" class="h-7 text-[11px]" @click="cancel" :disabled="saving">
                <X class="size-3 mr-1" /> Cancel
            </Button>
            <Button size="xs" class="h-7 text-[11px] bg-blue-600 text-white hover:bg-blue-700" @click="save" :disabled="saving">
                <Save class="size-3 mr-1" /> {{ saving ? 'Creating…' : 'Create' }}
            </Button>
        </div>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #f1f1f1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #e5e5e5; }
</style>
