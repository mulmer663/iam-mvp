export interface Department {
    id: string
    externalId?: string
    displayName: string
    description?: string
    active: boolean
    parentId: string | null
    meta?: {
        resourceType: string
        created: string
        lastModified: string
    }
}

export interface User {
    id: string
    externalId?: string
    userName: string
    name: {
        familyName: string
        givenName: string
        formatted?: string
    }
    title?: string
    active: boolean
    emails: Array<{ value: string; primary: boolean }>
    'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User'?: {
        employeeNumber?: string
        department?: string
        costCenter?: string
        organization?: string
        division?: string
    }
    meta?: {
        resourceType: string
        created: string
        lastModified: string
        location: string
    }
    [key: string]: any
}

export interface AttributeMapping {
    fromField: string
    toField: string
    fromLabel?: string
    toLabel?: string
    value?: string
    transformType?: string
    transformParams?: string
    isRequired?: boolean
}

export interface HistoryLog {
    id: string
    traceId: string
    eventType: 'USER_CREATE' | 'USER_UPDATE' | 'USER_UPDATE_SIMPLE' | 'USER_UPDATE_CRITICAL' | 'USER_RETIRE'
    status: 'SUCCESS' | 'FAILURE'
    target: string
    sourceSystem?: string
    targetSystem?: string
    syncDirection?: string
    time: string
    message?: string
    userId?: string
    userName?: string
    syncType?: 'JOIN' | 'REJOIN' | 'UPDATE_SIMPLE' | 'UPDATE_CRITICAL' | 'LEAVE'
    requestPayload?: Record<string, any>
    resultData?: Record<string, any>
    appliedRules?: number[]
    ruleRevId?: number
    // Computed/Derived for UI convenience
    payload?: Record<string, any> // Deprecated but kept for compatibility logic
    changes?: Array<{ field: string, old: string, new: string }>
    mappings?: AttributeMapping[]
    snapshot?: {
        layer: 'HR' | 'IAM' | 'AD'
        data: Record<string, any>
    }
}

export interface MillerPane {
    id: string
    type: string
    title: string
    data: any
    width?: string
    maxWidth?: string
}

export interface UserRevisionHistory {
    revId: number
    traceId: string
    operatorId: string
    operationType: string
    timestamp: string
    profile: User
}

export interface PagedResponse<T> {
    content: T[]
    totalElements: number
    totalPages: number
    size: number
    number: number
    numberOfElements: number
    first: boolean
    last: boolean
    empty: boolean
}
