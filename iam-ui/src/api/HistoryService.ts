import { request } from './client'
import type { HistoryLog } from '@/types'

interface HistoryResponse {
    id: string
    traceId: string
    type: string
    status: string
    target: string
    time: string
    message?: string
    payload?: string // JSON string
}

export const HistoryService = {
    async getHistory(): Promise<HistoryLog[]> {
        const response = await request<HistoryResponse[]>('/v1/history')
        return response.map(this.toHistoryLog)
    },

    toHistoryLog(dto: HistoryResponse): HistoryLog {
        let payloadObj = {}
        try {
            if (dto.payload) {
                payloadObj = JSON.parse(dto.payload)
            }
        } catch (e) {
            console.error('Failed to parse history payload', e)
        }

        return {
            id: dto.id,
            traceId: dto.traceId,
            type: dto.type as any,
            status: dto.status as any,
            target: dto.target,
            time: dto.time,
            message: dto.message,
            payload: payloadObj,
            // Flatten relevant fields from payload for UI compatibility
            ...(payloadObj as any)
        }
    }
}
