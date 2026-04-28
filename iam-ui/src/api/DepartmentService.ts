import { request } from './client'
import type { Department } from '@/types'

interface ScimListResponse<T> {
    totalResults: number
    Resources: T[]
}

let _cache: Department[] | null = null

async function fetchAll(): Promise<Department[]> {
    if (_cache) return _cache
    const res = await request<ScimListResponse<Department>>('/scim/v2/Departments')
    _cache = res.Resources
    return _cache
}

export const DepartmentService = {
    async getDepartments(): Promise<Department[]> {
        return fetchAll()
    },

    async getDepartment(id: string): Promise<Department | undefined> {
        const all = await fetchAll()
        return all.find(d => d.id === id)
    },

    async getSubDepartments(parentId: string | null): Promise<Department[]> {
        const all = await fetchAll()
        return all.filter(d => (d.parentId ?? null) === parentId)
    },

    invalidateCache() {
        _cache = null
    }
}
