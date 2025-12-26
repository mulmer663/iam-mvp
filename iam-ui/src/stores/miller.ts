import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MillerPane } from '@/types'

export const useMillerStore = defineStore('miller', () => {
    const highlightedPaneId = ref<string | null>(null)
    const activePaneId = ref<string | null>(null)
    const panes = ref<MillerPane[]>([])

    function pushPane(pane: MillerPane) {
        panes.value.push(pane)
        activePaneId.value = null
        activePaneId.value = pane.id
    }

    function popPane() {
        panes.value.pop()
    }

    function popToPane(id: string) {
        const index = panes.value.findIndex(p => p.id === id)
        if (index !== -1) {
            panes.value = panes.value.slice(0, index + 1)
        }
    }

    function setPane(index: number, pane: MillerPane) {
        panes.value = [...panes.value.slice(0, index), pane]
        activePaneId.value = null
        activePaneId.value = pane.id
    }

    let highlightTimeout: any = null

    function highlightPane(id: string) {
        if (highlightTimeout) clearTimeout(highlightTimeout)

        // Reset to trigger animation again if already highlighted
        highlightedPaneId.value = null

        setTimeout(() => {
            highlightedPaneId.value = id
            highlightTimeout = setTimeout(() => {
                if (highlightedPaneId.value === id) {
                    highlightedPaneId.value = null
                }
            }, 2000)
        }, 10)
    }

    return {
        panes,
        highlightedPaneId,
        activePaneId,
        pushPane,
        popPane,
        popToPane,
        setPane,
        highlightPane
    }
})
