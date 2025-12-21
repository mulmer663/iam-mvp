import { defineStore } from 'pinia'

export interface Pane {
    id: string
    type: string
    title: string
    data: any
    width?: string
}

export const useMillerStore = defineStore('miller', {
    state: () => ({
        panes: [] as Pane[],
        activePaneId: '' as string,
    }),
    actions: {
        pushPane(pane: Pane) {
            // If the pane is already open, focus it
            const existingIndex = this.panes.findIndex(p => p.id === pane.id)
            if (existingIndex !== -1) {
                this.popToPane(pane.id)
                return
            }

            this.panes.push(pane)
            this.activePaneId = pane.id
        },
        popToPane(paneId: string) {
            const index = this.panes.findIndex(p => p.id === paneId)
            if (index !== -1) {
                this.panes = this.panes.slice(0, index + 1)
                this.activePaneId = paneId
            }
        },
        updatePaneData(paneId: string, newData: any) {
            const pane = this.panes.find(p => p.id === paneId)
            if (pane) {
                pane.data = { ...pane.data, ...newData }
            }
        },
    },
})
