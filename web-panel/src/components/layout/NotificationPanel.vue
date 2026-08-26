<script setup lang="ts">
import { computed } from 'vue'
import type { NotificationItem } from '@/types'

interface Props {
  visible: boolean
  list?: NotificationItem[]
  unreadCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  list: () => [],
  unreadCount: 0
})

const emit = defineEmits<{
  goto: [item: NotificationItem]
  markAllRead: []
  close: []
}>()

function levelColor(level: string): string {
  switch (level) {
    case 'danger': return 'var(--danger-red)'
    case 'warning': return 'var(--warning)'
    case 'success': return 'var(--success)'
    default: return 'var(--accent-blue)'
  }
}

function formatTime(t: string): string {
  try {
    const d = new Date(t)
    const now = new Date()
    const diff = Math.floor((now.getTime() - d.getTime()) / 1000)
    if (diff < 60) return `${diff}秒前`
    if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
    if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
    return d.toLocaleDateString('zh-CN')
  } catch {
    return t
  }
}

const visibleItems = computed(() => props.list.slice(0, 8))
</script>

<template>
  <Transition name="slide-up">
    <div
      v-if="visible"
      class="fixed right-4 top-[68px] w-[360px] max-w-[92vw] z-[45] rounded-card border border-border-line bg-bg-card shadow-lg overflow-hidden"
      style="max-height: 560px;"
    >
      <!-- 头部 -->
      <div class="flex items-center justify-between px-4 py-3 border-b border-border-line">
        <h3 class="text-card-title text-text-primary m-0">通知中心</h3>
        <div class="flex items-center gap-1">
          <button
            type="button"
            class="text-xs text-accent-cyan hover:text-accent-cyan/80 px-2 py-1 rounded-btn hover:bg-accent-cyan/10 transition-colors"
            :disabled="unreadCount === 0"
            :class="unreadCount === 0 ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'"
            @click="emit('markAllRead')"
          >
            标记全部已读
          </button>
          <button
            type="button"
            class="btn btn-ghost btn-icon !w-7 !h-7 !p-0.5 ml-1"
            @click="emit('close')"
          >
            ×
          </button>
        </div>
      </div>

      <!-- 列表区 -->
      <div style="max-height: 480px;" class="overflow-y-auto">
        <div
          v-if="list.length === 0"
          class="p-10 text-center text-text-secondary text-sm"
        >
          暂无通知
        </div>

        <div v-else>
          <div
            v-for="n in visibleItems"
            :key="n.id"
            class="px-4 py-3 border-b border-border-line/60 hover:bg-bg-hover cursor-pointer flex gap-3 transition-colors last:border-b-0"
            :class="{ 'opacity-60': n.read }"
            @click="emit('goto', n)"
          >
            <!-- 级别色块 8px -->
            <span
              class="mt-1 w-2 h-2 rounded-full shrink-0"
              :style="{ background: levelColor(n.level) }"
            ></span>

            <div class="min-w-0 flex-1">
              <div class="flex items-start justify-between gap-2 mb-1">
                <span class="text-sm font-medium text-text-primary leading-tight">{{ n.title }}</span>
                <span class="text-[11px] text-text-muted shrink-0 whitespace-nowrap">
                  {{ formatTime(n.time) }}
                </span>
              </div>
              <p class="text-xs text-text-secondary m-0 leading-relaxed line-clamp-2">
                {{ n.content }}
              </p>
              <div class="mt-1 text-[11px] text-accent-cyan hover:underline">
                点击查看详情 →
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部 -->
      <div class="px-4 py-2.5 border-t border-border-line text-center">
        <button
          type="button"
          class="text-xs text-accent-cyan hover:underline transition-colors"
        >
          查看全部通知
        </button>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
