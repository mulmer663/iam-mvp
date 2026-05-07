<script setup lang="ts">
import { UserService } from '@/api/UserService'
import type { ScimPageResult } from '@/api/UserService'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Mail, MoreHorizontal, User as LucideUser, Search, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useMillerStore } from '@/stores/miller'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import type { User } from '@/types'

const props = defineProps<{
    deptId?: string
    paneIndex?: number
}>()

const emit = defineEmits<{
    (e: 'total-results', total: number): void
}>()

const millerStore = useMillerStore()

const page = ref<ScimPageResult<User>>({ items: [], totalResults: 0, startIndex: 1, itemsPerPage: 100 })
const searchQuery = ref('')
const currentStartIndex = ref(1)
const PAGE_SIZE = 50

let searchTimer: ReturnType<typeof setTimeout> | null = null
const isMounted = ref(false)

async function fetchUsers() {
    if (!isMounted.value) return
    const filter = searchQuery.value.trim()
        ? `userName co "${searchQuery.value.trim()}" or displayName co "${searchQuery.value.trim()}"`
        : undefined

    try {
        const result = await UserService.getUsers({
            filter,
            startIndex: currentStartIndex.value,
            count: PAGE_SIZE,
        })
        if (isMounted.value) {
            page.value = result
            emit('total-results', result.totalResults)
        }
    } catch (e) {
        if (isMounted.value) console.error('Failed to load users', e)
    }
}

onMounted(() => {
    isMounted.value = true
    fetchUsers()
})

onUnmounted(() => {
    isMounted.value = false
    if (searchTimer) clearTimeout(searchTimer)
})

// 검색어 변경 시 debounce 후 첫 페이지부터 재조회
watch(searchQuery, () => {
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(() => {
        currentStartIndex.value = 1
        fetchUsers()
    }, 300)
})

// 부서 필터 변경 시 재조회
watch(() => props.deptId, () => {
    currentStartIndex.value = 1
    fetchUsers()
})

// 부서 필터는 클라이언트 사이드 (department는 SCIM 필터 미지원)
const ENTERPRISE_URN = 'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User'
const filteredUsers = computed((): User[] => {
    if (!props.deptId) return page.value.items
    return page.value.items.filter(u => u[ENTERPRISE_URN]?.department === props.deptId)
})

// 페이지네이션
const canPrev = computed(() => currentStartIndex.value > 1)
const canNext = computed(() => currentStartIndex.value + PAGE_SIZE - 1 < page.value.totalResults)

function prevPage() {
    if (!canPrev.value) return
    currentStartIndex.value = Math.max(1, currentStartIndex.value - PAGE_SIZE)
    fetchUsers()
}

function nextPage() {
    if (!canNext.value) return
    currentStartIndex.value = currentStartIndex.value + PAGE_SIZE
    fetchUsers()
}

const pageLabel = computed(() => {
    const end = Math.min(currentStartIndex.value + PAGE_SIZE - 1, page.value.totalResults)
    return `${currentStartIndex.value}–${end} / ${page.value.totalResults}`
})

const selectedUserId = computed(() => {
    const detailPane = millerStore.panes.find(p => p.type === 'UserDetail' && p.data.user)
    return detailPane?.data.user.id
})

function openUserDetail(user: User) {
    const detailPaneId = `userdetail-${user.id}`
    const existingPane = millerStore.panes.find(p => p.id === detailPaneId)

    if (existingPane) {
        millerStore.highlightPane(detailPaneId)
        millerStore.activePaneId = detailPaneId
        return
    }

    const detailPane = {
        id: detailPaneId,
        type: 'UserDetail',
        title: `User: ${user.name.givenName} ${user.name.familyName}`,
        data: { user },
        width: 'w1'
    }

    if (typeof props.paneIndex === 'number') {
        millerStore.setPane(props.paneIndex + 1, detailPane)
    } else {
        millerStore.pushPane(detailPane)
    }
}
</script>

<template>
    <div class="h-full flex flex-col gap-2">
        <!-- 검색 입력 -->
        <div class="relative shrink-0">
            <Search class="absolute left-2 top-1/2 -translate-y-1/2 size-3 text-neutral-400 pointer-events-none" />
            <input
                v-model="searchQuery"
                type="text"
                placeholder="Search by name or username..."
                class="w-full h-7 bg-neutral-50 border border-neutral-100 rounded-sm pl-7 pr-3 text-[11px] focus:border-blue-300 focus:bg-white focus:outline-none"
            />
        </div>

        <!-- 테이블 -->
        <div class="flex-1 overflow-auto border rounded-md">
            <Table class="iam-table">
                <TableHeader class="bg-neutral-50/80 sticky top-0 z-10">
                    <TableRow class="hover:bg-transparent h-8">
                        <TableHead class="w-[30px] p-0 text-center">
                            <MoreHorizontal class="size-3 inline-block" />
                        </TableHead>
                        <TableHead class="text-[10px] font-extrabold uppercase p-2">Name</TableHead>
                        <TableHead class="text-[10px] font-extrabold uppercase p-2">Position</TableHead>
                        <TableHead class="text-[10px] font-extrabold uppercase p-2 text-center">Status</TableHead>
                        <TableHead class="text-[10px] font-extrabold uppercase p-2">Email</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    <TableRow
                        v-for="user in filteredUsers"
                        :key="user.id"
                        @click="openUserDetail(user)"
                        class="h-8 cursor-pointer group border-b border-neutral-50 transition-colors"
                        :class="[selectedUserId === user.id ? 'bg-blue-50/50' : 'hover:bg-neutral-50']"
                    >
                        <TableCell class="p-0 text-center">
                            <LucideUser class="size-3 inline-block text-neutral-300 group-hover:text-blue-500" />
                        </TableCell>
                        <TableCell class="p-2 py-1">
                            <div class="flex flex-col">
                                <span class="text-[12px] font-semibold text-neutral-800 leading-tight">
                                    {{ user.name.givenName }} {{ user.name.familyName }}
                                </span>
                                <span class="text-[10px] text-neutral-400 font-mono">{{ user.userName }}</span>
                            </div>
                        </TableCell>
                        <TableCell class="p-2 py-1">
                            <span class="text-[11px] text-neutral-600 font-medium">{{ user.title }}</span>
                        </TableCell>
                        <TableCell class="p-2 py-1 text-center">
                            <span
                                class="px-1.5 py-0 h-4 text-[9px] uppercase font-bold tracking-wider rounded-sm"
                                :class="user.active ? 'bg-blue-100 text-blue-700' : 'bg-neutral-100 text-neutral-400'"
                            >
                                {{ user.active ? 'ACTIVE' : 'INACTIVE' }}
                            </span>
                        </TableCell>
                        <TableCell class="p-2 py-1 relative">
                            <div class="flex items-center gap-1.5">
                                <Mail class="size-3 text-neutral-300" />
                                <span class="text-[11px] text-neutral-500">{{ user.emails?.[0]?.value || '-' }}</span>
                            </div>
                            <div v-if="selectedUserId === user.id" class="absolute right-0 top-0 bottom-0 w-0.5 bg-blue-600" />
                        </TableCell>
                    </TableRow>
                </TableBody>
            </Table>
        </div>

        <!-- 페이지네이션 -->
        <div v-if="page.totalResults > PAGE_SIZE" class="flex items-center justify-between shrink-0 px-1">
            <span class="text-[10px] text-neutral-400 font-mono">{{ pageLabel }}</span>
            <div class="flex items-center gap-1">
                <button
                    @click="prevPage"
                    :disabled="!canPrev"
                    class="p-0.5 rounded-sm hover:bg-neutral-100 disabled:opacity-30 disabled:cursor-not-allowed"
                >
                    <ChevronLeft class="size-3 text-neutral-500" />
                </button>
                <button
                    @click="nextPage"
                    :disabled="!canNext"
                    class="p-0.5 rounded-sm hover:bg-neutral-100 disabled:opacity-30 disabled:cursor-not-allowed"
                >
                    <ChevronRight class="size-3 text-neutral-500" />
                </button>
            </div>
        </div>
    </div>
</template>

<style>
.iam-table {
    border-collapse: separate;
    border-spacing: 0;
}
.iam-table th, .iam-table td {
    border-bottom-width: 1px;
}
</style>
