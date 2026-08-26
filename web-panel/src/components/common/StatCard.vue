<script setup lang="ts">
import { computed } from 'vue'

interface BadgeProp {
  type: 'warning' | 'danger' | 'success' | 'info'
  text: string
}

interface Props {
  title: string
  value: string | number
  trend?: number
  badge?: BadgeProp
  ring?: number
  ringColor?: string
}

const props = withDefaults(defineProps<Props>(), {
  ringColor: '#00E5FF'
})

const badgeIcon = computed(() => {
  if (!props.badge) return ''
  switch (props.badge.type) {
    case 'warning': return '⚠'
    case 'danger': return '🚨'
    case 'success': return '✓'
    case 'info': return 'ℹ'
    default: return ''
  }
})

const badgeClass = computed(() => {
  if (!props.badge) return ''
  switch (props.badge.type) {
    case 'warning': return 'tag-yellow'
    case 'danger': return 'tag-red'
    case 'success': return 'tag-green'
    case 'info': return 'tag-blue'
    default: return 'tag-gray'
  }
})

// SVG 环形进度条
const R = 22
const C = 2 * Math.PI * R // ≈ 138
const ringOffset = computed(() => {
  const pct = Math.max(0, Math.min(100, props.ring ?? 0))
  return C - (pct / 100) * C
})

const trendColor = computed(() => {
  if (props.trend === undefined) return ''
  return props.trend >= 0 ? 'var(--success)' : 'var(--danger-red)'
})

const trendIcon = computed(() => {
  if (props.trend === undefined) return ''
  return props.trend >= 0 ? '↗' : '↘'
})
</script>

<template>
  <div class="rounded-card border border-border-line bg-bg-card p-3">
    <div class="flex flex-col">
      <!-- 标题 -->
      <div class="text-card-title text-text-secondary mb-1">{{ title }}</div>

      <!-- 主数值 + 右侧 ring -->
      <div class="flex items-start justify-between gap-3">
        <div class="text-[28px] font-semibold text-text-primary leading-tight">
          {{ value }}
        </div>

        <!-- Ring：互斥优先级最高 -->
        <div v-if="ring !== undefined" class="shrink-0">
          <svg :width="56" :height="56" viewBox="0 0 56 56">
            <!-- 底弧 -->
            <circle
              cx="28" cy="28" :r="R"
              fill="none"
              stroke="var(--bg-hover)"
              stroke-width="5"
            />
            <!-- 主弧 -->
            <circle
              cx="28" cy="28" :r="R"
              fill="none"
              :stroke="ringColor"
              stroke-width="5"
              stroke-linecap="round"
              transform="rotate(-90 28 28)"
              :stroke-dasharray="C"
              :stroke-dashoffset="ringOffset"
              style="transition: stroke-dashoffset 0.4s ease"
            />
          </svg>
        </div>
      </div>

      <!-- 底部辅助信息：trend / badge -->
      <div class="mt-2 flex items-center gap-2">
        <template v-if="ring === undefined && trend !== undefined">
          <span
            class="text-xs font-semibold"
            :style="{ color: trendColor }"
          >
            {{ trendIcon }} {{ Math.abs(trend) }}%
          </span>
          <span class="text-xs text-text-muted">vs 前日</span>
        </template>
        <template v-else-if="ring === undefined && badge">
          <span class="tag" :class="badgeClass">
            <span>{{ badgeIcon }}</span>
            <span>{{ badge.text }}</span>
          </span>
        </template>
      </div>
    </div>
  </div>
</template>
