export interface ResourceTypeMetadata {
    id: string;
    name: string;
    endpoint: string;
    description: string;
    schema: string;
    schemaExtensions?: Array<{
        schema: string;
        required: boolean;
    }>;
}

export const SCIM_RESOURCE_TYPES: Record<string, ResourceTypeMetadata> = {
    "urn:ietf:params:scim:schemas:core:2.0:User": {
        "id": "User",
        "name": "User",
        "endpoint": "/Users",
        "description": "User Account",
        "schema": "urn:ietf:params:scim:schemas:core:2.0:User",
        "schemaExtensions": [
            {
                "schema": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                "required": false
            }
        ]
    },
    "urn:ietf:params:scim:schemas:core:2.0:Group": {
        "id": "Group",
        "name": "Group",
        "endpoint": "/Groups",
        "description": "Group Resource",
        "schema": "urn:ietf:params:scim:schemas:core:2.0:Group"
    },
    "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
        "id": "EnterpriseUser",
        "name": "Enterprise User Extension",
        "endpoint": "/Users",
        "description": "Enterprise User Extension",
        "schema": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
    }
};

export const getMetadataBySchema = (schemaUri: string): ResourceTypeMetadata | null => {
    return SCIM_RESOURCE_TYPES[schemaUri] || null;
};
