import { request } from './client'
import type { User } from '@/types'

export interface ScimQueryParams {
    filter?: string
    startIndex?: number
    count?: number
}

export interface ScimPageResult<T> {
    items: T[]
    totalResults: number
    startIndex: number
    itemsPerPage: number
}

interface ScimListResponse<T> {
    schemas: string[]
    totalResults: number
    itemsPerPage: number
    startIndex: number
    Resources: T[]
}

interface ScimUserResponse {
    schemas: string[]
    id: string
    externalId?: string
    userName: string
    name: {
        familyName: string
        givenName: string
        formatted?: string
    }
    title?: string
    emails: { value: string; primary?: boolean; type?: string }[]
    active: boolean
    meta: {
        resourceType: string
        created: string
        lastModified: string
        location: string
    }
    [key: string]: any
}

export interface ScimPatchOp {
    op: 'add' | 'replace' | 'remove'
    path?: string
    value?: any
}

const SCIM_PATCH_SCHEMA = 'urn:ietf:params:scim:api:messages:2.0:PatchOp'
const SCIM_USER_SCHEMA = 'urn:ietf:params:scim:schemas:core:2.0:User'

function buildQuery(params: ScimQueryParams): string {
    const q = new URLSearchParams()
    if (params.filter)     q.set('filter',     params.filter)
    if (params.startIndex) q.set('startIndex', String(params.startIndex))
    if (params.count !== undefined) q.set('count', String(params.count))
    const str = q.toString()
    return str ? `?${str}` : ''
}

export const UserService = {
    async getUsers(params: ScimQueryParams = {}): Promise<ScimPageResult<User>> {
        const query = buildQuery({ startIndex: 1, count: 100, ...params })
        const response = await request<ScimListResponse<ScimUserResponse>>(`/scim/v2/Users${query}`)
        return {
            items: (response.Resources ?? []).map(this.toUser),
            totalResults: response.totalResults,
            startIndex: response.startIndex,
            itemsPerPage: response.itemsPerPage,
        }
    },

    async getUser(id: string): Promise<User> {
        const response = await request<ScimUserResponse>(`/scim/v2/Users/${id}`)
        return this.toUser(response)
    },

    async createUser(payload: Record<string, any>): Promise<User> {
        const schemas: string[] = [SCIM_USER_SCHEMA]
        for (const key of Object.keys(payload)) {
            if (key.startsWith('urn:') && !schemas.includes(key)) schemas.push(key)
        }
        const body = { schemas, ...payload }
        const response = await request<ScimUserResponse>('/scim/v2/Users', {
            method: 'POST',
            body: JSON.stringify(body)
        })
        return this.toUser(response)
    },

    async patchUser(id: string, operations: ScimPatchOp[]): Promise<User> {
        const body = {
            schemas: [SCIM_PATCH_SCHEMA],
            Operations: operations
        }
        const response = await request<ScimUserResponse>(`/scim/v2/Users/${id}`, {
            method: 'PATCH',
            body: JSON.stringify(body)
        })
        return this.toUser(response)
    },

    async deleteUser(id: string): Promise<void> {
        await request<void>(`/scim/v2/Users/${id}`, { method: 'DELETE' })
    },

    toUser(scim: ScimUserResponse): User {
        const user: User = {
            id: scim.id,
            externalId: scim.externalId,
            userName: scim.userName,
            name: scim.name,
            title: scim.title || '',
            active: scim.active,
            emails: (scim.emails || []).map(e => ({ value: e.value, primary: e.primary ?? false })),
            meta: scim.meta
        }
        for (const key of Object.keys(scim)) {
            if (key.startsWith('urn:')) user[key] = scim[key]
        }
        return user
    }
}
