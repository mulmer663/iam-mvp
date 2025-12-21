export const MOCK_DEPARTMENTS = [
    { id: 'DEPT01', name: 'IAM Solution Group', parentId: null },
    { id: 'DEPT01-1', name: 'Core Development Team', parentId: 'DEPT01' },
    { id: 'DEPT01-2', name: 'Connector Team', parentId: 'DEPT01' },
    { id: 'DEPT02', name: 'Business Strategy', parentId: null },
    { id: 'DEPT02-1', name: 'Marketing Team', parentId: 'DEPT02' },
]

export const MOCK_USERS = [
    { id: '1', loginId: 'admin', name: 'System Administrator', deptCode: 'DEPT01', status: 'ACTIVE', position: 'Manager', email: 'admin@iam.com' },
    { id: '2', loginId: 'hong.g', name: 'Gildong Hong', deptCode: 'DEPT01-1', status: 'ACTIVE', position: 'Senior Engineer', email: 'hong@iam.com' },
    { id: '3', loginId: 'kim.f', name: 'Free Kim', deptCode: 'DEPT01-2', status: 'ACTIVE', position: 'Junior Engineer', email: 'kim@iam.com' },
    { id: '4', loginId: 'lee.p', name: 'Planner Lee', deptCode: 'DEPT02-1', status: 'INACTIVE', position: 'Associate', email: 'lee@iam.com' },
]

export const MOCK_HISTORY = [
    // Hong Gildong (id: 2)
    { id: 'evt-001', traceId: 'tr-001', type: 'HR_SYNC', status: 'SUCCESS', target: 'Hong Gildong', time: '2025-01-01 09:00:00', userId: '2', syncType: 'JOIN' },
    { id: 'evt-004', traceId: 'tr-004', type: 'HR_SYNC', status: 'SUCCESS', target: 'Hong Gildong', time: '2025-06-15 14:00:00', userId: '2', syncType: 'UPDATE_SIMPLE' },
    { id: 'evt-005', traceId: 'tr-005', type: 'HR_SYNC', status: 'SUCCESS', target: 'Hong Gildong', time: '2025-12-21 10:05:00', userId: '2', syncType: 'UPDATE_CRITICAL' },
    { id: 'evt-007', traceId: 'tr-007', type: 'AD_PROVISION', status: 'SUCCESS', target: 'Hong Gildong', time: '2025-12-21 10:30:00', userId: '2' },

    // Free Kim (id: 3)
    { id: 'evt-002', traceId: 'tr-002', type: 'AD_PROVISION', status: 'PENDING', target: 'Free Kim', time: '2025-12-21 10:30:00', userId: '3' },
    { id: 'evt-006', traceId: 'tr-006', type: 'HR_SYNC', status: 'FAILURE', target: 'Free Kim', time: '2025-12-21 10:25:00', userId: '3', syncType: 'UPDATE_SIMPLE' },

    // System Admin (id: 1)
    { id: 'evt-003', traceId: 'tr-003', type: 'USER_UPDATE', status: 'SUCCESS', target: 'System Administrator', time: '2025-12-21 11:15:00', userId: '1' },
]
