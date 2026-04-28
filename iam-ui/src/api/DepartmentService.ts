import { request } from './client'
import type { Department } from '@/types'

interface ScimListResponse<T> {
    totalResults: number
    Resources: T[]
}

interface ScimDepartmentResponse {
    id: string
    externalId?: string
    displayName: string
    description?: string
    active: boolean
    parentId: string | null
    meta?: { resourceType: string; created: string; lastModified: string }
}

function toDepartment(d: ScimDepartmentResponse): Department {
    return {
        id: d.id,
        externalId: d.externalId,
        displayName: d.displayName,
        description: d.description,
        active: d.active,
        parentId: d.parentId ?? null,
        meta: d.meta
    }
}

// Module-level cache — invalidated on page reload (create-drop DB는 어차피 재시작마다 리셋)
let _cache: Department[] | null = null

async function fetchAll(): Promise<Department[]> {
    if (_cache) return _cache
    const res = await request<ScimListResponse<ScimDepartmentResponse>>('/scim/v2/Departments')
    _cache = res.Resources.map(toDepartment)
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
        return all.filter(d => d.parentId === parentId)
    },

    invalidateCache() {
        _cache = null
    }
}
