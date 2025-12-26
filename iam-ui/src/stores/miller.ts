import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MillerPane } from '@/types'

export const useMillerStore = defineStore('miller', () => {
    const highlightedPaneId = ref<string | null>(null)
    const panes = ref<MillerPane[]>([])

    function pushPane(pane: MillerPane) {
        panes.value.push(pane)
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
    }

    function highlightPane(id: string) {
        highlightedPaneId.value = id
        setTimeout(() => {
            if (highlightedPaneId.value === id) {
                highlightedPaneId.value = null
            }
        }, 2000)
    }

    return {
        panes,
        highlightedPaneId,
        pushPane,
        popPane,
        popToPane,
        setPane,
        highlightPane
    }
})
