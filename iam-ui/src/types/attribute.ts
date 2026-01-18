export type AttributeCategory = 'CORE' | 'EXTENSION'
export type AttributeDataType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE' | 'CODE'
export type AttributeMutability = 'READ_ONLY' | 'READ_WRITE' | 'WRITE_ONCE' | 'IMMUTABLE'
export type AttributeTargetDomain = 'USER' | 'DEPARTMENT' | 'GROUP'

export interface IamAttributeMeta {
    code: string
    targetDomain: AttributeTargetDomain
    category: AttributeCategory
    displayName: string
    dataType: AttributeDataType
    scimSchemaUri?: string
    description?: string
    required: boolean
    mutability: AttributeMutability
    adminOnly: boolean
    viewLevel: number
    editLevel: number
    encrypted: boolean
    uiComponent?: string
}
