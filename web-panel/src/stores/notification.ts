import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { NotificationItem } from '@/types'
import notificationsJson from '@/api/mock/notifications.json'
import { delay } from '@/utils/mockDelay'

const LS_READ_IDS = 'anticheat_notif_read_ids'

export const useNotificationStore = defineStore('notification', () => {
  // ---------- state ----------
  const list = ref<NotificationItem[]>([])
  const loaded = ref(false)
  const loading = ref(false)

  // ---------- hydrate read flag ----------
  let readIds = new Set<string>()
  try {
    const raw = localStorage.getItem(LS_READ_IDS)
    if (raw) readIds = new Set(JSON.parse(raw) as string[])
  } catch { /* noop */ }

  function persistRead(): void {
    try {
      localStorage.setItem(LS_READ_IDS, JSON.stringify(Array.from(readIds)))
    } catch { /* noop */ }
  }

  function applyReadFlags(items: NotificationItem[]): NotificationItem[] {
    return items.map((n) => ({ ...n, read: n.read || readIds.has(n.id) }))
  }

  // ---------- getters ----------
  const unreadCount = computed<number>(() => list.value.filter((n) => !n.read).length)

  // ---------- actions ----------
  async function fetchList(): Promise<void> {
    if (loaded.value) return
    loading.value = true
    try {
      await delay()
      list.value = applyReadFlags(notificationsJson as NotificationItem[]).sort(
        (a, b) => (a.time < b.time ? 1 : -1)
      )
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  function markRead(id: string): void {
    const item = list.value.find((n) => n.id === id)
    if (item && !item.read) {
      item.read = true
      readIds.add(id)
      persistRead()
    }
  }

  function markAllRead(): void {
    list.value.forEach((n) => {
      if (!n.read) {
        n.read = true
        readIds.add(n.id)
      }
    })
    persistRead()
  }

  /** 对外: 模拟新通知到达 (可被 WS 消息驱动调用) */
  function pushNotification(n: NotificationItem): void {
    list.value.unshift(applyReadFlags([n])[0])
  }

  function clearAll(): void {
    list.value = []
    readIds = new Set()
    persistRead()
  }

  return {
    // state
    list,
    loaded,
    loading,
    // getters
    unreadCount,
    // actions
    fetchList,
    markRead,
    markAllRead,
    pushNotification,
    clearAll
  }
})
