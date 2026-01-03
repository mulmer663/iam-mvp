import { request } from './client'
import type { HistoryLog } from '@/types'

interface HistoryResponse {
    id: string
    traceId: string
    type: string
    status: string
    target: string // targetUser
    sourceSystem?: string
    targetSystem?: string
    time: string // Align with time in api-specs.md
    message?: string
    payload?: string // JSON string
    requestPayload?: string
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
        const syncType = payloadObj.syncType ||
            (['HR_SYNC', 'USER_CREATE', 'USER_SYNC'].includes(dto.type) ? 'JOIN' : undefined);
        const snapshot = payloadObj.snapshot || undefined;
        const mappings = payloadObj.mappings || undefined;
        const changes = payloadObj.changes || undefined;
        const actualPayload = payloadObj.snapshot ? payloadObj.snapshot.data : payloadObj;

        // Ensure time is formatted nicely (remove T if present)
        const formattedTime = dto.time ? dto.time.replace('T', ' ') : '';

        return {
            ...(actualPayload || {}),
            id: dto.id,
            traceId: dto.traceId,
            type: dto.type as any,
            status: dto.status as any,
            target: dto.target,
            sourceSystem: dto.sourceSystem,
            targetSystem: dto.targetSystem,
            time: formattedTime,
            message: dto.message,
            payload: actualPayload,
            syncType: syncType as any,
            snapshot: snapshot,
            mappings: mappings,
            changes: changes,
            requestPayload: dto.requestPayload
        };
    }
}



