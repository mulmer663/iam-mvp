<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Users as UsersIcon, Save, X } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useMillerStore } from '@/stores/miller'
import { useAttributeStore } from '@/stores/attribute'
import { useGroupStore } from '@/stores/group'
import { toast } from '@/utils/toast'
import type { IamAttributeMeta } from '@/types/attribute'

const props = defineProps<{ paneIndex?: number }>()

const millerStore = useMillerStore()
const attrStore = useAttributeStore()
const groupStore = useGroupStore()

const saving = ref(false)
const formData = ref<Record<string, any>>({})

const createAttrs = computed(() =>
    attrStore.groupAttributes.filter(a => !a.parentName && a.mutability !== 'READ_ONLY' && a.type !== 'COMPLEX')
)

onMounted(async () => {
    if (attrStore.groupAttributes.length === 0) await attrStore.fetchAttributes('GROUP')
    initBlank()
})

function initBlank() {
    const blank: Record<string, any> = {}
    for (const a of createAttrs.value) {
        if (a.type === 'BOOLEAN') blank[a.name] = false
        else blank[a.name] = ''
    }
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
    if (!formData.value.displayName?.trim()) {
        toast.error('Display Name is required')
        return
    }
    saving.value = true
    try {
        const payload = cleanPayload(formData.value)
        const created = await groupStore.create(payload)
        toast.success(`Group created: ${created.displayName}`)
        millerStore.setPane(props.paneIndex ?? 1, {
            id: `group-detail-${created.id}`,
            type: 'GroupDetailPane',
            title: created.displayName,
            data: { groupId: created.id },
            width: 'w1'
        })
    } catch (e: any) {
        toast.error(e?.message || 'Failed to create group')
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
                <UsersIcon class="size-3.5 text-blue-500" />
            </div>
            <div class="min-w-0">
                <div class="font-bold text-neutral-800 text-[13px]" data-testid="group-create-title">New Group</div>
                <div class="text-[10px] text-neutral-400">Fields derived from Group Attribute Schema</div>
            </div>
        </div>

        <!-- Form -->
        <div class="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar">
            <template v-for="attr in createAttrs" :key="attr.name">
                <div>
                    <label class="block text-[9px] font-bold text-neutral-400 uppercase tracking-tighter mb-0.5">
                        {{ attr.displayName }}
                        <span v-if="attr.required" class="text-red-400 ml-0.5">*</span>
                    </label>
                    <label v-if="attr.type === 'BOOLEAN'" class="flex items-center gap-2 cursor-pointer h-7">
                        <input type="checkbox" v-model="formData[attr.name]"
                            class="rounded border-neutral-300 text-blue-600" />
                        <span class="text-[11px] text-neutral-600">{{ formData[attr.name] ? 'Yes' : 'No' }}</span>
                    </label>
                    <textarea v-else-if="attr.uiComponent === 'textarea'"
                        v-model="formData[attr.name]"
                        :placeholder="attr.description ?? attr.displayName"
                        rows="2"
                        class="w-full px-2 py-1.5 text-[11px] bg-white border border-neutral-200 rounded focus:border-blue-400 focus:ring-1 focus:ring-blue-100 outline-none transition-all resize-none"
                    />
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
