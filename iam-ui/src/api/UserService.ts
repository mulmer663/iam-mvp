import { request } from './client'
import type { User } from '@/types'

// SCIM Types from Backend (matching ScimListResponse & ScimUserResponse)
interface ScimListResponse<T> {
    totalResults: number
    itemsPerPage: number
    startIndex: number
    Resources: T[]
}

interface ScimUserResponse {
    id: string
    userName: string
    name: {
        familyName: string
        givenName: string
        formatted?: string
    }
    emails: { value: string; primary: boolean }[]
    active: boolean
    'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User'?: {
        department?: string
        employeeNumber?: string
        costCenter?: string
        organization?: string
        division?: string
    }
}

export const UserService = {
    async getUsers(): Promise<User[]> {
        const response = await request<ScimListResponse<ScimUserResponse>>('/scim/v2/Users')
        return response.Resources.map(this.toUser)
    },

    async getUser(id: string): Promise<User> {
        const response = await request<ScimUserResponse>(`/scim/v2/Users/${id}`)
        return this.toUser(response)
    },

    // Convert Backend SCIM Response to Frontend User Type
    toUser(scim: ScimUserResponse): User {
        return {
            id: scim.id,
            userName: scim.userName,
            name: scim.name,
            title: '', // Title is not standard SCIM Core but often in Enterprise extension or custom. Backend map logic check needed.
            // Wait, Backend UserQueryService maps everything standard. 
            // Let's assume title comes from enterprise extension or is missing for now.
            // Update: Checked backend, title is in IamUser but NOT mapped to SCIM standard fields in UserQueryService yet.
            // We will map it if available or leave empty.
            active: scim.active,
            emails: scim.emails,
            'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': scim['urn:ietf:params:scim:schemas:extension:enterprise:2.0:User']
        }
    }
}
