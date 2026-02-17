import { toast as sonnerToast } from "vue-sonner"

export const toast = {
    success: (message: string, description?: string) => {
        sonnerToast.success(message, {
            description,
            duration: 3000,
        })
    },
    error: (message: string, description?: string) => {
        sonnerToast.error(message, {
            description,
            duration: 5000,
        })
    },
    info: (message: string, description?: string) => {
        sonnerToast(message, {
            description,
            duration: 3000,
        })
    },
    warning: (message: string, description?: string) => {
        sonnerToast.warning(message, {
            description,
            duration: 4000,
        })
    },
}
