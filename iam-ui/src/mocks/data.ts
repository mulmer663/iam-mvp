export const MOCK_DEPARTMENTS: any[] = [
    {
        id: 'GLOBAL-IT', displayName: 'Global IT', parentId: null, active: true, externalId: 'ORG-001',
        meta: { resourceType: 'Group', created: '2024-01-01T00:00:00Z', lastModified: '2024-01-01T00:00:00Z' }
    },
    {
        id: 'IT-INFRA', displayName: 'Infrastructure', parentId: 'GLOBAL-IT', active: true, externalId: 'ORG-002',
        meta: { resourceType: 'Group', created: '2024-01-01T00:00:00Z', lastModified: '2024-01-01T00:00:00Z' }
    },
    {
        id: 'IT-SEC', displayName: 'Cyber Security', parentId: 'GLOBAL-IT', active: true, externalId: 'ORG-003',
        meta: { resourceType: 'Group', created: '2024-01-01T00:00:00Z', lastModified: '2024-01-01T00:00:00Z' }
    },
    {
        id: 'IT-APPS', displayName: 'Business Applications', parentId: 'GLOBAL-IT', active: true, externalId: 'ORG-004',
        meta: { resourceType: 'Group', created: '2024-01-01T00:00:00Z', lastModified: '2024-01-01T00:00:00Z' }
    },
    {
        id: 'AUDIT-01', displayName: 'Internal Audit', parentId: null, active: true, externalId: 'ORG-005',
        meta: { resourceType: 'Group', created: '2024-01-01T00:00:00Z', lastModified: '2024-01-01T00:00:00Z' }
    },
    {
        id: 'SEC-OPS', displayName: 'Security Operations', parentId: 'IT-SEC', active: true, externalId: 'ORG-006',
        meta: { resourceType: 'Group', created: '2024-01-01T00:00:00Z', lastModified: '2024-01-01T00:00:00Z' }
    },
    {
        id: 'EXTERNAL-V', displayName: 'External Vendors', parentId: null, active: true, externalId: 'ORG-007',
        meta: { resourceType: 'Group', created: '2024-01-01T00:00:00Z', lastModified: '2024-01-01T00:00:00Z' }
    },
    {
        id: 'DEPT01', displayName: 'SAP HR Division', parentId: null, active: true, externalId: 'ORG-008',
        meta: { resourceType: 'Group', created: '2024-01-01T00:00:00Z', lastModified: '2024-01-01T00:00:00Z' }
    },
    {
        id: 'DEPT02', displayName: 'SAP HR Planning', parentId: 'DEPT01', active: true, externalId: 'ORG-009',
        meta: { resourceType: 'Group', created: '2024-01-01T00:00:00Z', lastModified: '2024-01-01T00:00:00Z' }
    },
    {
        id: 'DEPT03', displayName: 'SAP HR Operations', parentId: 'DEPT01', active: true, externalId: 'ORG-010',
        meta: { resourceType: 'Group', created: '2024-01-01T00:00:00Z', lastModified: '2024-01-01T00:00:00Z' }
    },
]

export const MOCK_USERS: any[] = [
    {
        id: '1',
        userName: 'super.admin',
        name: { familyName: 'Admin', givenName: 'Michael' },
        title: 'IT Director',
        active: true,
        emails: [{ value: 'michael.admin@global-iam.com', primary: true }],
        'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': {
            department: 'GLOBAL-IT',
            employeeNumber: 'ADM001'
        }
    },
    {
        id: '2',
        userName: 'jane.doe',
        name: { familyName: 'Doe', givenName: 'Jane' },
        title: 'External Auditor',
        active: true,
        emails: [{ value: 'jane.doe@audit-firm.com', primary: true }],
        'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': {
            department: 'AUDIT-01',
            employeeNumber: 'EXT-101'
        }
    },
    {
        id: '3',
        userName: 'john.smith',
        name: { familyName: 'Smith', givenName: 'John' },
        title: 'Security Analyst',
        active: true,
        emails: [{ value: 'john.smith@global-iam.com', primary: true }],
        'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': {
            department: 'SEC-OPS',
            employeeNumber: 'SEC-888'
        }
    },
    {
        id: '4',
        userName: 'sarah.v',
        name: { familyName: 'Vendor', givenName: 'Sarah' },
        title: 'Implementation Partner',
        active: false,
        emails: [{ value: 'sarah.v@partner.com', primary: true }],
        'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': {
            department: 'EXTERNAL-V',
            employeeNumber: 'VND-444'
        }
    },
]

export const MOCK_HISTORY = [
    // --- Scenario: New Employee Join (Jane Doe) ---
    {
        id: '101', traceId: 'T-101', type: 'HR_SYNC', status: 'SUCCESS', targetUser: 'jane.doe', time: '2025-01-01 09:00:00', userId: '2', syncType: 'JOIN',
        snapshot: {
            layer: 'HR',
            data: { empId: 'EXT-101', name: 'Jane Doe', position: 'Auditor', dept: 'AUDIT-01' }
        },
        mappings: [
            { fromLabel: 'HR', toLabel: 'IAM', fromField: 'position', toField: 'title', value: 'External Auditor' }
        ]
    },
    {
        id: '102', traceId: 'T-101', type: 'USER_UPDATE', status: 'SUCCESS', targetUser: 'jane.doe', time: '2025-01-01 09:00:05', userId: '2', syncType: 'JOIN',
        snapshot: {
            layer: 'IAM',
            data: { id: '2', userName: 'jane.doe', active: true }
        }
    },
    {
        id: '103', traceId: 'T-101', type: 'AD_PROVISION', status: 'SUCCESS', targetUser: 'jane.doe', time: '2025-01-01 09:01:00', userId: '2', syncType: 'JOIN',
        snapshot: {
            layer: 'AD',
            data: { sAMAccountName: 'jane.doe', displayName: 'Jane Doe' }
        },
        mappings: [
            { fromLabel: 'IAM', toLabel: 'AD', fromField: 'title', toField: 'title', value: 'External Auditor' }
        ]
    },

    // --- Scenario: Promotion (Jane Doe) ---
    {
        id: '201', traceId: 'T-201', type: 'HR_SYNC', status: 'SUCCESS', targetUser: 'jane.doe', time: '2025-12-21 10:05:00', userId: '2', syncType: 'UPDATE_CRITICAL',
        changes: [{ field: 'position', old: 'Auditor', new: 'Senior Auditor' }],
        snapshot: {
            layer: 'HR',
            data: { empId: 'EXT-101', name: 'Jane Doe', position: 'Senior Auditor', dept: 'AUDIT-01' }
        },
        mappings: [{ fromLabel: 'HR', toLabel: 'IAM', fromField: 'position', toField: 'title', value: 'Senior Auditor' }]
    },
    {
        id: '202', traceId: 'T-201', type: 'USER_UPDATE', status: 'SUCCESS', targetUser: 'jane.doe', time: '2025-12-21 10:05:05', userId: '2', syncType: 'UPDATE_CRITICAL',
        changes: [{ field: 'title', old: 'External Auditor', new: 'Senior Auditor' }],
        snapshot: {
            layer: 'IAM',
            data: { id: '2', title: 'Senior Auditor' }
        }
    },
    {
        id: '203', traceId: 'T-201', type: 'AD_PROVISION', status: 'SUCCESS', targetUser: 'jane.doe', time: '2025-12-21 10:05:30', userId: '2', syncType: 'UPDATE_CRITICAL',
        changes: [{ field: 'title', old: 'External Auditor', new: 'Senior Auditor' }],
        snapshot: {
            layer: 'AD',
            data: { sAMAccountName: 'jane.doe', title: 'Senior Auditor' }
        },
        mappings: [{ fromLabel: 'IAM', toLabel: 'AD', fromField: 'title', toField: 'title', value: 'Senior Auditor' }]
    }
]

