export const MOCK_DEPARTMENTS = [
    { id: 'DEPT01', name: 'IAM Solution Group', parentId: null },
    { id: 'DEPT01-1', name: 'Core Development Team', parentId: 'DEPT01' },
    { id: 'DEPT01-2', name: 'Connector Team', parentId: 'DEPT01' },
    { id: 'DEPT02', name: 'Business Strategy', parentId: null },
    { id: 'DEPT02-1', name: 'Marketing Team', parentId: 'DEPT02' },
]

export const MOCK_USERS = [
    { id: '1', loginId: 'admin', name: 'System Administrator', deptCode: 'DEPT01', status: 'ACTIVE', position: 'Manager', email: 'admin@iam.com' },
    { id: '532100000000000002', loginId: 'hong.g', name: 'Gildong Hong', deptCode: 'DEPT01-1', status: 'ACTIVE', position: 'Senior Engineer', email: 'hong@iam.com' },
    { id: '3', loginId: 'kim.f', name: 'Free Kim', deptCode: 'DEPT01-2', status: 'ACTIVE', position: 'Junior Engineer', email: 'kim@iam.com' },
    { id: '4', loginId: 'lee.p', name: 'Planner Lee', deptCode: 'DEPT02-1', status: 'INACTIVE', position: 'Associate', email: 'lee@iam.com' },
]

export const MOCK_HISTORY = [
    // --- Transaction 1: New Employee Join (Hong Gildong) ---
    // HR Step: Original Data (No mapping here, logically it's the source)
    {
        id: '532100000000000001', traceId: '532100000000000001', type: 'HR_SYNC', status: 'SUCCESS', target: 'Hong Gildong', time: '2025-01-01 09:00:00', userId: '532100000000000002', syncType: 'JOIN',
        snapshot: {
            layer: 'HR',
            data: { empId: 'H001', name: 'Hong Gildong', position: 'Senior Engineer', dept: 'Dev Team', email: 'hong@test.com' }
        }
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
        },
        mappings: [
            { fromLabel: 'HR', toLabel: 'IAM', fromField: 'position', toField: 'title', value: 'Senior Engineer' },
            { fromLabel: 'HR', toLabel: 'IAM', fromField: 'dept', toField: 'department', value: 'Dev Team' }
        ]
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
        }
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
        },
        mappings: [{ fromLabel: 'HR', toLabel: 'IAM', fromField: 'position', toField: 'title', value: 'Principal Engineer' }]
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
