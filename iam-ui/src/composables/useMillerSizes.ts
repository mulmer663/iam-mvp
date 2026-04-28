import { onMounted, onUnmounted, readonly, ref, type Ref } from 'vue'

const PEEK = 24   // px visible for LVL.N+1 hint
const GAP  = 8    // gap-2 between panes

export type WidthToken = 'w1' | 'w2' | 'w3'

interface MillerSizes {
    w1: number
    w2: number
    w3: number
}

const sizes = ref<MillerSizes>({ w1: 340, w2: 688, w3: 1036 })

function compute(containerWidth: number): MillerSizes {
    const w1 = Math.floor((containerWidth - PEEK - GAP * 2) / 3)
    return { w1, w2: w1 * 2 + GAP, w3: w1 * 3 + GAP * 2 }
}

export function useMillerSizes(containerRef: Ref<HTMLElement | null>) {
    let ro: ResizeObserver | null = null

    onMounted(() => {
        if (!containerRef.value) return
        ro = new ResizeObserver((entries) => {
            if (entries[0]) sizes.value = compute(entries[0].contentRect.width)
        })
        ro.observe(containerRef.value)
        sizes.value = compute(containerRef.value.clientWidth)
    })

    onUnmounted(() => ro?.disconnect())

    function resolveWidth(token: string | undefined): string {
        if (token === 'w1') return sizes.value.w1 + 'px'
        if (token === 'w2') return sizes.value.w2 + 'px'
        if (token === 'w3') return sizes.value.w3 + 'px'
        return token ?? sizes.value.w1 + 'px'
    }

    return { sizes: readonly(sizes), resolveWidth }
}
