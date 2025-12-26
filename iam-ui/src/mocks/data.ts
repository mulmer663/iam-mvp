export const MOCK_DEPARTMENTS = [
    { id: 'DEPT01', name: 'IAM Solution Group', parentId: null },
    { id: 'DEPT01-1', name: 'Dev Team', parentId: 'DEPT01' },
    { id: 'DEPT01-2', name: 'Connector Team', parentId: 'DEPT01' },
    { id: 'DEPT02', name: 'Business Strategy', parentId: null },
    { id: 'DEPT02-1', name: 'Marketing Team', parentId: 'DEPT02' },
]

export const MOCK_USERS: any[] = [
    {
        id: '1',
        userName: 'admin',
        name: { familyName: 'System', givenName: 'Administrator' },
        title: 'Manager',
        active: true,
        emails: [{ value: 'admin@iam.com', primary: true }],
        'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': { department: 'DEPT01' }
    },
    {
        id: '532100000000000002',
        userName: 'hong.g',
        name: { familyName: 'Hong', givenName: 'Gildong' },
        title: 'Principal Engineer',
        active: true,
        emails: [{ value: 'hong@test.com', primary: true }],
        'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': { department: 'DEPT01-1', employeeNumber: 'H001' }
    },
    {
        id: '3',
        userName: 'kim.f',
        name: { familyName: 'Kim', givenName: 'Free' },
        title: 'Junior Engineer',
        active: true,
        emails: [{ value: 'kim@iam.com', primary: true }],
        'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': { department: 'DEPT01-2' }
    },
    {
        id: '4',
        userName: 'lee.p',
        name: { familyName: 'Lee', givenName: 'Planner' },
        title: 'Associate',
        active: false,
        emails: [{ value: 'lee@iam.com', primary: true }],
        'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': { department: 'DEPT02-1' }
    },
]

export const MOCK_HISTORY = [
    // --- Transaction 1: New Employee Join (Hong Gildong) ---
    // HR Step: Original Data (No mapping here, logically it's the source)
    {
        id: '532100000000000001', traceId: '532100000000000001', type: 'HR_SYNC', status: 'SUCCESS', target: 'Hong Gildong', time: '2025-01-01 09:00:00', userId: '532100000000000002', syncType: 'JOIN',
        snapshot: {
            layer: 'HR',
            data: { empId: 'H001', name: 'Hong Gildong', position: 'Senior Engineer', dept: 'Dev Team', email: 'hong@test.com' }
        },
        mappings: [
            { fromLabel: 'HR', toLabel: 'IAM', fromField: 'position', toField: 'title', value: 'Senior Engineer' },
            { fromLabel: 'HR', toLabel: 'IAM', fromField: 'dept', toField: 'department', value: 'Dev Team' }
        ]
    },
    // IAM Step: Ingestion Mapping (HR <-> IAM)
    {
        id: '532100000000000002', traceId: '532100000000000001', type: 'USER_UPDATE', status: 'SUCCESS', target: 'Hong Gildong', time: '2025-01-01 09:00:05', userId: '532100000000000002', syncType: 'JOIN',
        snapshot: {
            layer: 'IAM',
            data: {
                schemas: [
                    "urn:ietf:params:scim:schemas:core:2.0:User",
                    "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                    "urn:ietf:params:scim:schemas:extension:mycustom:2.0:User"
                ],
                id: '532100000000000002',
                userName: 'hong.g@iam.com',
                name: { familyName: 'Hong', givenName: 'Gildong' },
                title: 'Senior Engineer',
                active: true,
                emails: [{ value: 'hong@test.com', primary: true }],
                'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': {
                    employeeNumber: 'H001',
                    department: 'Dev Team'
                },
                'urn:ietf:params:scim:schemas:extension:mycustom:2.0:User': {
                    birthday: '1990-01-01',
                    theme: 'dark'
                }
            }
        }
    },
    // Target Step: Distribution Mapping (IAM <-> AD)
    {
        id: '532100000000000003', traceId: '532100000000000001', type: 'AD_PROVISION', status: 'SUCCESS', target: 'Hong Gildong', time: '2025-01-01 09:01:00', userId: '532100000000000002', syncType: 'JOIN',
        snapshot: {
            layer: 'AD',
            data: { sAMAccountName: 'hong.g', displayName: 'Hong Gildong', title: 'Senior Engineer', mail: 'hong@test.com', description: 'Dev Team' }
        },
        mappings: [
            { fromLabel: 'IAM', toLabel: 'AD', fromField: 'title', toField: 'title', value: 'Senior Engineer' },
            { fromLabel: 'IAM', toLabel: 'AD', fromField: 'department', toField: 'description', value: 'Dev Team' }
        ]
    },

    // --- Transaction 3: Critical Update (Promotion) ---
    {
        id: '542100000000000021', traceId: '542100000000000150', type: 'HR_SYNC', status: 'SUCCESS', target: 'Hong Gildong', time: '2025-12-21 10:05:00', userId: '532100000000000002', syncType: 'UPDATE_CRITICAL',
        changes: [{ field: 'position', old: 'Senior Engineer', new: 'Principal Engineer' }],
        snapshot: {
            layer: 'HR',
            data: { empId: 'H001', name: 'Hong Gildong', position: 'Principal Engineer', dept: 'Dev Team', email: 'hong@test.com' }
        },
        mappings: [{ fromLabel: 'HR', toLabel: 'IAM', fromField: 'position', toField: 'title', value: 'Principal Engineer' }]
    },
    {
        id: '542100000000000022', traceId: '542100000000000150', type: 'USER_UPDATE', status: 'SUCCESS', target: 'Hong Gildong', time: '2025-12-21 10:05:05', userId: '532100000000000002', syncType: 'UPDATE_CRITICAL',
        changes: [{ field: 'title', old: 'Senior Engineer', new: 'Principal Engineer' }],
        snapshot: {
            layer: 'IAM',
            data: {
                id: '532100000000000002',
                userName: 'hong.g@iam.com',
                name: { familyName: 'Hong', givenName: 'Gildong' },
                title: 'Principal Engineer',
                active: true,
                emails: [{ value: 'hong@test.com', primary: true }],
                'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User': { employeeNumber: 'H001', department: 'Dev Team' }
            }
        }
    },
    {
        id: '542100000000000023', traceId: '542100000000000150', type: 'AD_PROVISION', status: 'SUCCESS', target: 'Hong Gildong', time: '2025-12-21 10:05:30', userId: '532100000000000002', syncType: 'UPDATE_CRITICAL',
        changes: [{ field: 'title', old: 'Senior Engineer', new: 'Principal Engineer' }],
        snapshot: {
            layer: 'AD',
            data: { sAMAccountName: 'hong.g', displayName: 'Hong Gildong', title: 'Principal Engineer', mail: 'hong@test.com', description: 'Dev Team' }
        },
        mappings: [{ fromLabel: 'IAM', toLabel: 'AD', fromField: 'title', toField: 'title', value: 'Principal Engineer' }]
    }
]
