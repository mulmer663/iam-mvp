import { Database, Activity, Server, History } from 'lucide-vue-next'

export const SYSTEM_THEMES = {
    SOURCE: {
        label: 'Source Sync',
        subLabel: 'HR Source System',
        icon: Database,
        color: 'blue',
        bg: 'bg-blue-50',
        border: 'border-blue-200',
        text: 'text-blue-700',
        indicator: 'bg-blue-500',
        container: 'bg-blue-50/30 border-blue-200 ring-1 ring-blue-100 shadow-sm'
    },
    INTEGRATION: {
        label: 'Integration Sync',
        subLabel: 'AD Target System',
        icon: Server,
        color: 'purple',
        bg: 'bg-purple-50',
        border: 'border-purple-200',
        text: 'text-purple-700',
        indicator: 'bg-purple-500',
        container: 'bg-purple-50/30 border-purple-200 ring-1 ring-purple-100 shadow-sm'
    },
    AUDIT: {
        label: 'Modification Ledger',
        subLabel: 'IAM Core (SCIM 2.0)',
        icon: History,
        color: 'amber',
        bg: 'bg-amber-50',
        border: 'border-amber-200',
        text: 'text-amber-700',
        indicator: 'bg-amber-500',
        container: 'bg-amber-50/30 border-amber-200 ring-1 ring-amber-100 shadow-sm'
    },
    IAM: {
        label: 'IAM Core (SCIM 2.0)',
        icon: Activity,
        color: 'orange',
        bg: 'bg-orange-50',
        border: 'border-orange-200',
        text: 'text-orange-700',
        indicator: 'bg-orange-500',
        container: 'bg-orange-50/30 border-orange-200 ring-1 ring-orange-100 shadow-sm'
    }
} as const

export type SystemType = keyof typeof SYSTEM_THEMES
