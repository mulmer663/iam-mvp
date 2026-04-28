<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Save, X, UserPlus } from 'lucide-vue-next'
import { useAttributeStore } from '@/stores/attribute'
import { useMillerStore } from '@/stores/miller'
import { UserService } from '@/api/UserService'
import { toast } from '@/utils/toast'
import DynamicUserForm from '@/components/user/DynamicUserForm.vue'

const props = defineProps<{ paneIndex?: number }>()

const attrStore = useAttributeStore()
const millerStore = useMillerStore()

const formData = ref<Record<string, any>>({})
const saving = ref(false)

const userAttributes = computed(() => attrStore.userAttributes)

onMounted(async () => {
    if (attrStore.userAttributes.length === 0) {
        await attrStore.fetchAttributes('USER')
    }
    initBlank()
})

function initBlank() {
    const blank: Record<string, any> = {}
    for (const a of userAttributes.value) {
        if (a.parentName) continue
        if (a.type === 'COMPLEX' && a.multiValued) blank[a.name] = []
        else if (a.type === 'COMPLEX') blank[a.name] = {}
        else if (a.type === 'BOOLEAN') blank[a.name] = false
        else blank[a.name] = ''
        if (a.category === 'EXTENSION' && a.scimSchemaUri) {
            if (!blank[a.scimSchemaUri]) blank[a.scimSchemaUri] = {}
            const target = blank[a.scimSchemaUri]
            if (a.type === 'COMPLEX' && a.multiValued) target[a.name] = []
            else if (a.type === 'COMPLEX') target[a.name] = {}
            else if (a.type === 'BOOLEAN') target[a.name] = false
            else target[a.name] = ''
            // Remove the duplicated top-level entry for extensions
            delete blank[a.name]
        }
    }
    formData.value = blank
}

function cleanPayload(obj: Record<string, any>): Record<string, any> {
    const out: Record<string, any> = {}
    for (const k of Object.keys(obj)) {
        const v = obj[k]
        if (v === '' || v === null || v === undefined) continue
        if (Array.isArray(v) && v.length === 0) continue
        if (typeof v === 'object' && !Array.isArray(v)) {
            const nested = cleanPayload(v)
            if (Object.keys(nested).length === 0) continue
            out[k] = nested
        } else {
            out[k] = v
        }
    }
    return out
}

async function save() {
    saving.value = true
    try {
        const payload = cleanPayload(formData.value)
        if (!payload.userName) {
            toast.error('userName is required')
            saving.value = false
            return
        }
        const created = await UserService.createUser(payload)
        toast.success(`User created: ${created.userName}`)
        // Replace current pane with detail view
        if (typeof props.paneIndex === 'number') {
            millerStore.setPane(props.paneIndex, {
                id: `userdetail-${created.id}`,
                type: 'UserDetail',
                title: `User: ${created.name?.givenName ?? ''} ${created.name?.familyName ?? ''}`,
                data: { user: created },
                width: '800px',
                maxWidth: '800px'
            })
        }
    } catch (e: any) {
        console.error(e)
        toast.error(e?.message || 'Failed to create user')
    } finally {
        saving.value = false
    }
}

function cancel() {
    if (typeof props.paneIndex === 'number') {
        millerStore.removePane(props.paneIndex)
    }
}
</script>

<template>
    <div class="h-full flex flex-col bg-white text-[12px]">

        <!-- Header -->
        <div class="border-b border-neutral-100 bg-neutral-50/60 px-4 py-3 shrink-0 flex items-center gap-2">
            <div class="size-7 rounded-md bg-blue-50 border border-blue-100 flex items-center justify-center">
                <UserPlus class="size-3.5 text-blue-500" />
            </div>
            <div class="min-w-0">
                <div class="font-bold text-neutral-800 text-[13px]" data-testid="user-create-title">New User</div>
                <div class="text-[10px] text-neutral-400">Fields are derived from User Attribute Schema</div>
            </div>
        </div>

        <!-- Body -->
        <div class="flex-1 overflow-y-auto p-4">
            <div v-if="attrStore.loading" class="text-center text-neutral-300 italic py-8">Loading…</div>
            <DynamicUserForm v-else
                v-model="formData"
                :attributes="userAttributes"
                mode="create" />
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
