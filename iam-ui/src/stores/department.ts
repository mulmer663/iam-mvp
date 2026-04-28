import { defineStore } from 'pinia'
import { ref } from 'vue'
import { DepartmentService } from '@/api/DepartmentService'
import type { Department } from '@/types'

export const useDeptStore = defineStore('department', () => {
    const departments = ref<Department[]>([])
    const loading = ref(false)

    async function fetchAll() {
        loading.value = true
        try {
            DepartmentService.invalidateCache()
            departments.value = await DepartmentService.getDepartments()
        } finally {
            loading.value = false
        }
    }

    async function create(payload: Record<string, any>): Promise<Department> {
        const created = await DepartmentService.createDepartment(payload)
        await fetchAll()
        return created
    }

    async function update(id: string, payload: Record<string, any>): Promise<Department> {
        const updated = await DepartmentService.updateDepartment(id, payload)
        await fetchAll()
        return updated
    }

    async function remove(id: string): Promise<void> {
        await DepartmentService.deleteDepartment(id)
        departments.value = departments.value.filter(d => d.id !== id)
    }

    return { departments, loading, fetchAll, create, update, remove }
})
