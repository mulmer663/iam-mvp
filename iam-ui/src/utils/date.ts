export function formatDateTime(dateStr: string | undefined | null) {
    if (!dateStr) return '-'

    // Replace 'T' with a space and remove microseconds/milliseconds
    // Result format: YYYY-MM-DD HH:mm:ss
    return dateStr.replace('T', ' ').split('.')[0]
}
