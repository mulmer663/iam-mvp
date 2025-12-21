import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MillerPane } from '@/types'

export const useMillerStore = defineStore('miller', () => {
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

    return {
        panes,
        pushPane,
        popPane,
        popToPane,
        setPane
    }
})
