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
        indicator: 'bg-blue-500'
    },
    INTEGRATION: {
        label: 'Integration Sync',
        subLabel: 'AD Target System',
        icon: Server,
        color: 'purple',
        bg: 'bg-purple-50',
        border: 'border-purple-200',
        text: 'text-purple-700',
        indicator: 'bg-purple-500'
    },
    AUDIT: {
        label: 'Modification Ledger',
        subLabel: 'IAM Core (SCIM 2.0)',
        icon: History,
        color: 'amber',
        bg: 'bg-amber-50',
        border: 'border-amber-200',
        text: 'text-amber-700',
        indicator: 'bg-amber-500'
    },
    IAM: {
        label: 'IAM Core (SCIM 2.0)',
        icon: Activity,
        color: 'orange',
        bg: 'bg-orange-50',
        border: 'border-orange-200',
        text: 'text-orange-700',
        indicator: 'bg-orange-500'
    }
} as const

export type SystemType = keyof typeof SYSTEM_THEMES
