import { defineStore } from 'pinia'
import { ref } from 'vue'
import { GroupService, type Group } from '@/api/GroupService'

export const useGroupStore = defineStore('group', () => {
    const groups = ref<Group[]>([])
    const loading = ref(false)

    async function fetchAll() {
        loading.value = true
        try {
            groups.value = await GroupService.getGroups()
        } finally {
            loading.value = false
        }
    }

    async function create(payload: Record<string, any>): Promise<Group> {
        const created = await GroupService.createGroup(payload)
        await fetchAll()
        return created
    }

    async function update(id: string, payload: Record<string, any>): Promise<Group> {
        const updated = await GroupService.updateGroup(id, payload)
        await fetchAll()
        return updated
    }

    async function remove(id: string): Promise<void> {
        await GroupService.deleteGroup(id)
        groups.value = groups.value.filter(g => g.id !== id)
    }

    return { groups, loading, fetchAll, create, update, remove }
})
