import { request } from './client'
import type { HistoryLog } from '@/types'

interface HistoryResponse {
    id: string
    traceId: string
    type: string
    status: string
    targetUser: string // Matched with Backend SyncHistory entity
    createdAt: string // Matched with Backend DTO
    message?: string
    payload?: string // JSON string
}

export const HistoryService = {
    async getHistory(filters?: { userId?: string, userName?: string }): Promise<HistoryLog[]> {
        let url = '/v1/history';
        if (filters) {
            const params = new URLSearchParams();
            if (filters.userId) params.append('userId', filters.userId);
            if (filters.userName) params.append('targetUser', filters.userName);
            const query = params.toString();
            if (query) url += `?${query}`;
        }
        const response = await request<HistoryResponse[]>(url);
        return response.map(dto => this.toHistoryLog(dto));
    },

    toHistoryLog(dto: HistoryResponse): HistoryLog {
        let payloadObj: any = {};
        try {
            if (dto.payload) {
                payloadObj = JSON.parse(dto.payload);
            }
        } catch (e) {
            console.error('Failed to parse history payload', e);
        }

        // Handle rich wrapping (syncType, snapshot) if present
        const syncType = payloadObj.syncType || (dto.type === 'HR_SYNC' ? 'JOIN' : undefined);
        const snapshot = payloadObj.snapshot || undefined;
        const actualPayload = payloadObj.snapshot ? payloadObj.snapshot.data : payloadObj;

        // Ensure time is formatted nicely (remove T if present)
        const formattedTime = dto.createdAt ? dto.createdAt.replace('T', ' ') : '';

        return {
            id: dto.id,
            traceId: dto.traceId,
            type: dto.type as any,
            status: dto.status as any,
            target: dto.targetUser,
            time: formattedTime,
            message: dto.message,
            payload: actualPayload,
            syncType: syncType as any,
            snapshot: snapshot,
            // Flatten relevant fields from payload for UI compatibility
            ...(actualPayload || {})
        };
    }
}


