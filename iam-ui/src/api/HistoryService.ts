import {request} from './client'
import type {AttributeMapping, HistoryLog, PagedResponse, UserRevisionHistory} from '@/types'

interface HistoryResponse {
    id: string
    traceId: string
    eventType: string
    status: string
    target: string // targetUser
    sourceSystem?: string
    targetSystem?: string
    syncDirection?: string
    time: string
    message?: string
    requestPayload?: Record<string, any>
    resultData?: Record<string, any>
    appliedRules?: number[]
    ruleRevId?: number
    // Computed/Derived for UI convenience
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
        const response = await request<PagedResponse<HistoryResponse>>(url);
        // Page object wraps results in 'content'
        return response.content.map(dto => this.toHistoryLog(dto));
    },
    async getUserRevisionHistory(filters: { userId?: string, traceId?: string, page?: number, size?: number }): Promise<PagedResponse<UserRevisionHistory>> {
        const params = new URLSearchParams();
        if (filters.userId) params.append('userId', filters.userId);
        if (filters.traceId) params.append('traceId', filters.traceId);
        if (filters.page !== undefined) params.append('page', filters.page.toString());
        if (filters.size !== undefined) params.append('size', filters.size.toString());

        const url = `/v1/history/users?${params.toString()}`;
        return await request<PagedResponse<UserRevisionHistory>>(url);
    },

    async getRuleMappingHistory(systemId: string, revId: string | number): Promise<AttributeMapping[]> {
        const url = `/v1/rules/history?systemId=${systemId}&revId=${revId}`;
        const raw = await request<any[]>(url);

        // Map backend format (sourceField/targetField) to UI format (fromField/toField)
        return raw.map(m => ({
            fromField: m.sourceField,
            toField: m.targetField,
            transformType: m.transformType,
            transformParams: m.transformScript || m.transformParams,
            isRequired: m.isRequired
        } as any)); // Using any temporarily as we'll expand the interface if needed
    },

    toHistoryLog(dto: HistoryResponse): HistoryLog {
        const resultData = dto.resultData || {};
        const requestPayload = dto.requestPayload || {};

        // 1. Extract specific UI helpers from resultData if available
        const changes = resultData.changes || undefined;
        const syncType = resultData.syncType || dto.eventType;

        // 2. Mappings are no longer stored in payload, so we might not have them
        // unless we fetch them or they are legacy. 
        // We will leave mappings undefined for now (UI shows appliedRules instead).
        const mappings = resultData.mappings || undefined;

        // 3. Snapshot logic
        // For Join, requestPayload IS the snapshot data
        const snapshotData = requestPayload;

        // Ensure time is formatted nicely (remove T if present)
        const formattedTime = dto.time ? dto.time.replace('T', ' ') : '';

        return {
            id: dto.id,
            traceId: dto.traceId,
            eventType: dto.eventType as any,
            status: dto.status as any,
            target: dto.target,
            sourceSystem: dto.sourceSystem,
            targetSystem: dto.targetSystem,
            syncDirection: dto.syncDirection,
            time: formattedTime,
            message: dto.message,

            // Core Data
            requestPayload: dto.requestPayload,
            resultData: dto.resultData,
            appliedRules: dto.appliedRules,
            ruleRevId: dto.ruleRevId,

            // UI Helpers
            syncType: syncType as any,
            changes: changes,
            mappings: mappings,

            // Legacy Compatibility / UI View Helpers
            payload: resultData, // Some UI parts use payload as the "result" or "main data"
            snapshot: snapshotData ? {
                layer: (dto.sourceSystem || 'HR') as any,
                data: snapshotData
            } : undefined
        };
    }
}



