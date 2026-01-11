import {onMounted, onUnmounted, ref, type Ref} from 'vue'

export function useContainerWidth(elementRef: Ref<HTMLElement | null>) {
    const containerWidth = ref(0)
    const resizeObserver = ref<ResizeObserver | null>(null)

    onMounted(() => {
        if (!elementRef.value) return

        resizeObserver.value = new ResizeObserver((entries) => {
            for (const entry of entries) {
                containerWidth.value = entry.contentRect.width
            }
        })

        resizeObserver.value.observe(elementRef.value)
    })

    onUnmounted(() => {
        if (resizeObserver.value) {
            resizeObserver.value.disconnect()
        }
    })

    return {
        containerWidth
    }
}
