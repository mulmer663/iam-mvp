import { MOCK_DEPARTMENTS, MOCK_USERS } from "@/mocks/data";
import type { Department, User } from "@/types";

export const DepartmentService = {
    async getDepartments(): Promise<Department[]> {
        return MOCK_DEPARTMENTS;
    },

    async getDepartment(id: string): Promise<Department | undefined> {
        return MOCK_DEPARTMENTS.find(d => d.id === id);
    },

    async getSubDepartments(parentId: string | null): Promise<Department[]> {
        return MOCK_DEPARTMENTS.filter(d => d.parentId === parentId);
    },

    async getDepartmentMembers(deptId: string): Promise<User[]> {
        return MOCK_USERS.filter(u => u['urn:ietf:params:scim:schemas:extension:enterprise:2.0:User']?.department === deptId);
    }
};
