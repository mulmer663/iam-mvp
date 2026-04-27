<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { useResourceTypeStore } from '@/stores/resourceType'
import { useMillerStore } from '@/stores/miller'
import { toast } from '@/utils/toast'

const props = defineProps<{ paneIndex?: number }>()

const store = useResourceTypeStore()
const millerStore = useMillerStore()

const id = ref('urn:')          // full URN — user types the suffix
const name = ref('')
const description = ref('')
const saving = ref(false)
const error = ref('')

async function onSubmit() {
    error.value = ''
    if (!id.value || id.value === 'urn:' || !name.value) {
        error.value = 'Schema URN and name are required.'
        return
    }
    if (!id.value.startsWith('urn:')) {
        error.value = 'Schema id must be a valid URN starting with "urn:".'
        return
    }
    saving.value = true
    try {
        await store.createSchema({ id: id.value, name: name.value, description: description.value })
        toast.success(`Schema "${name.value}" created`)
        // Replace this pane with the new schema's detail pane
        millerStore.setPane(props.paneIndex ?? 0, {
            id: `schema-${id.value}`,
            type: 'SchemaDetailPane',
            title: name.value,
            width: '560px',
            data: { schemaId: id.value, paneIndex: props.paneIndex ?? 0 }
        })
    } catch (e: any) {
        error.value = e?.response?.data?.message ?? 'Failed to create schema'
    } finally {
        saving.value = false
    }
}
</script>

<template>
    <div class="h-full flex flex-col bg-white">
        <div class="h-9 border-b border-neutral-100 flex items-center px-3 bg-neutral-50/50 shrink-0">
            <span class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">New Extension Schema</span>
        </div>

        <form @submit.prevent="onSubmit" class="flex-1 overflow-y-auto p-4 space-y-4">
            <!-- URN -->
            <div class="space-y-1">
                <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">Schema URN *</label>
                <input v-model="id"
                    placeholder="urn:com:example:scim:schemas:extension:1.0:User"
                    class="w-full text-xs font-mono border border-neutral-200 rounded px-2 py-1.5 outline-none focus:border-blue-400" />
                <p class="text-[10px] text-neutral-400">
                    Must be a unique URN. Example: <code class="font-mono">urn:acme:scim:schemas:extension:1.0:User</code>
                </p>
            </div>

            <!-- Name -->
            <div class="space-y-1">
                <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">Name *</label>
                <input v-model="name"
                    placeholder="AcmeUser"
                    class="w-full text-sm border border-neutral-200 rounded px-2 py-1.5 outline-none focus:border-blue-400" />
            </div>

            <!-- Description -->
            <div class="space-y-1">
                <label class="text-[11px] font-bold text-neutral-600 uppercase tracking-wider">Description</label>
                <textarea v-model="description" rows="3"
                    placeholder="Acme Corp user extension schema"
                    class="w-full text-xs border border-neutral-200 rounded px-2 py-1.5 outline-none focus:border-blue-400 resize-none" />
            </div>

            <div v-if="error" class="text-xs text-red-500 bg-red-50 border border-red-200 rounded px-2 py-1.5">
                {{ error }}
            </div>

            <div class="flex gap-2 pt-2">
                <Button type="submit" class="h-8 text-xs px-4" :disabled="saving">
                    {{ saving ? 'Creating…' : 'Create Schema' }}
                </Button>
                <Button type="button" variant="outline" class="h-8 text-xs px-3"
                    @click="millerStore.removePane(paneIndex ?? 0)">
                    Cancel
                </Button>
            </div>
        </form>
    </div>
</template>
