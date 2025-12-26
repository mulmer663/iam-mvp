const BASE_URL = import.meta.env.VITE_API_URL || '/api'

export async function request<T>(path: string, options?: RequestInit): Promise<T> {
    const url = path.startsWith('/scim') ? path : `${BASE_URL}${path}`
    const response = await fetch(url, {
        headers: {
            'Content-Type': 'application/json',
            ...options?.headers
        },
        ...options
    })

    if (!response.ok) {
        throw new Error(`API Request failed: ${response.statusText}`)
    }

    if (response.status === 204) {
        return {} as T
    }

    return response.json()
}
