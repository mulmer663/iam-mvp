import { request } from './client'
import type { Department } from '@/types'

const DEPT_SCHEMA = 'urn:iam:params:scim:schemas:2.0:Department'

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

    async createDepartment(payload: Record<string, any>): Promise<Department> {
        const body = { schemas: [DEPT_SCHEMA], ...payload }
        const created = await request<Department>('/scim/v2/Departments', {
            method: 'POST',
            body: JSON.stringify(body)
        })
        _cache = null
        return created
    },

    async updateDepartment(id: string, payload: Record<string, any>): Promise<Department> {
        const body = { schemas: [DEPT_SCHEMA], ...payload }
        const updated = await request<Department>(`/scim/v2/Departments/${encodeURIComponent(id)}`, {
            method: 'PUT',
            body: JSON.stringify(body)
        })
        _cache = null
        return updated
    },

    async deleteDepartment(id: string): Promise<void> {
        await request<void>(`/scim/v2/Departments/${encodeURIComponent(id)}`, { method: 'DELETE' })
        _cache = null
    },

    invalidateCache() {
        _cache = null
    }
}
