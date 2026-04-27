<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Plus, Trash2 } from 'lucide-vue-next'
import { useResourceTypeStore } from '@/stores/resourceType'
import { useMillerStore } from '@/stores/miller'
import { toast } from '@/utils/toast'

const props = defineProps<{ paneIndex?: number }>()

const store = useResourceTypeStore()
const millerStore = useMillerStore()

const id = ref('')
const name = ref('')
const description = ref('')
const endpoint = ref('/')
const schema = ref('urn:')
const extensions = ref<Array<{ schema: string; required: boolean }>>([])
const saving = ref(false)
const error = ref('')

function addExtension() {
    extensions.value.push({ schema: '', required: false })
}

function removeExtension(idx: number) {
    extensions.value.splice(idx, 1)
}

async function onSubmit() {
    error.value = ''
    if (!id.value.trim() || !name.value.trim() || !endpoint.value.trim() || !schema.value || schema.value === 'urn:') {
        error.value = 'ID, Name, Endpoint, and Base Schema are required.'
        return
    }
    if (!schema.value.startsWith('urn:')) {
        error.value = 'Base Schema must be a valid URN starting with "urn:".'
        return
    }
    saving.value = true
    try {
        await store.createResourceType({
            id: id.value.trim(),
            name: name.value.trim(),
            description: description.value.trim(),
            endpoint: endpoint.value.trim(),
            schema: schema.value.trim(),
            schemaExtensions: extensions.value.filter(e => e.schema.trim())
        })
        toast.success(`Resource Type "${name.value}" created`)
        millerStore.setPane(props.paneIndex ?? 0, {
            id: `rt-${id.value}`,
            type: 'ResourceTypeDetailPane',
            title: name.value,
            width: '480px',
            data: { resourceTypeId: id.value, paneIndex: props.paneIndex ?? 0 }
        })
    } catch (e: any) {
        error.value = e?.response?.data?.message ?? 'Failed to create Resource Type'
    } finally {
        saving.value = false
    }
}
</script>

<template>
    <div class="h-full flex flex-col bg-white">
        <div class="h-9 border-b border-neutral-100 flex items-center px-3 bg-neutral-50/50 shrink-0">
            <span class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">New Resource Type</span>
        </div>

        <form @submit.prevent="onSubmit" class="flex-1 overflow-y-auto p-4 space-y-4">

            <div class="grid grid-cols-2 gap-3">
                <!-- ID -->
                <div class="space-y-1">
                    <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">ID *</label>
                    <input v-model="id" placeholder="e.g. Employee"
                        class="w-full text-xs border border-neutral-200 rounded px-2 py-1.5 outline-none focus:border-blue-400" />
                </div>
                <!-- Name -->
                <div class="space-y-1">
                    <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">Name *</label>
                    <input v-model="name" placeholder="e.g. Employee Account"
                        class="w-full text-xs border border-neutral-200 rounded px-2 py-1.5 outline-none focus:border-blue-400" />
                </div>
            </div>

            <!-- Description -->
            <div class="space-y-1">
                <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">Description</label>
                <input v-model="description" placeholder="Optional description"
                    class="w-full text-xs border border-neutral-200 rounded px-2 py-1.5 outline-none focus:border-blue-400" />
            </div>

            <!-- Endpoint -->
            <div class="space-y-1">
                <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">Endpoint *</label>
                <input v-model="endpoint" placeholder="/Employees"
                    class="w-full text-xs font-mono border border-neutral-200 rounded px-2 py-1.5 outline-none focus:border-blue-400" />
                <p class="text-[10px] text-neutral-400">SCIM path, e.g. <code class="font-mono">/Employees</code></p>
            </div>

            <!-- Base Schema -->
            <div class="space-y-1">
                <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">Base Schema URN *</label>
                <input v-model="schema" placeholder="urn:ietf:params:scim:schemas:core:2.0:User"
                    class="w-full text-xs font-mono border border-neutral-200 rounded px-2 py-1.5 outline-none focus:border-blue-400" />
            </div>

            <!-- Extensions -->
            <div class="space-y-2">
                <div class="flex items-center justify-between">
                    <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">Schema Extensions</label>
                    <Button type="button" size="xs" variant="ghost" class="h-6 text-[11px] text-blue-600" @click="addExtension">
                        <Plus class="size-3 mr-1" /> Add
                    </Button>
                </div>

                <div v-if="extensions.length === 0"
                    class="text-[10px] text-neutral-300 italic text-center py-3 border border-dashed border-neutral-200 rounded">
                    No extensions. Click "Add" to attach a schema.
                </div>

                <div v-for="(ext, idx) in extensions" :key="idx"
                    class="flex items-start gap-2 p-2 bg-neutral-50/50 border border-neutral-100 rounded group">
                    <div class="flex-1 space-y-1.5">
                        <input v-model="ext.schema" placeholder="urn:ietf:params:scim:schemas:extension:..."
                            class="w-full text-[11px] font-mono border border-neutral-200 rounded px-2 py-1 outline-none focus:border-blue-400" />
                        <label class="flex items-center gap-1.5 cursor-pointer">
                            <input type="checkbox" v-model="ext.required" class="size-3" />
                            <span class="text-[11px] text-neutral-500">Required</span>
                        </label>
                    </div>
                    <Button type="button" variant="ghost" size="icon"
                        class="size-6 mt-1 hover:bg-red-50 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                        @click="removeExtension(idx)">
                        <Trash2 class="size-3" />
                    </Button>
                </div>
            </div>

            <div v-if="error" class="text-xs text-red-500 bg-red-50 border border-red-200 rounded px-2 py-1.5">
                {{ error }}
            </div>

            <div class="flex gap-2 pt-2">
                <Button type="submit" class="h-8 text-xs px-4" :disabled="saving">
                    {{ saving ? 'Creating…' : 'Create Resource Type' }}
                </Button>
                <Button type="button" variant="outline" class="h-8 text-xs px-3"
                    @click="millerStore.removePane(paneIndex ?? 0)">
                    Cancel
                </Button>
            </div>
        </form>
    </div>
</template>
