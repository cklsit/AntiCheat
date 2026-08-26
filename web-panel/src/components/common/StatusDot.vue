<script setup lang="ts">
import { computed } from 'vue'

type DotStatus = 'online' | 'offline' | 'banned' | 'watching' | 'reconnecting' | 'connected' | 'disconnected'

interface Props {
  status?: DotStatus
  size?: 'sm' | 'md' | 'lg'
  label?: string
}

const props = withDefaults(defineProps<Props>(), {
  status: 'online',
  size: 'md',
  label: undefined
})

const dotClass = computed(() => {
  const s = props.status
  if (s === 'connected') return 'online'
  if (s === 'disconnected') return 'offline'
  if (s === 'reconnecting') return 'watching'
  return s
})

const sizePx = computed(() => {
  switch (props.size) {
    case 'sm': return '6px'
    case 'lg': return '12px'
    default: return '8px'
  }
})
</script>

<template>
  <span class="inline-flex items-center gap-2 align-middle">
    <span
      class="status-dot"
      :class="dotClass"
      :style="{ width: sizePx, height: sizePx }"
    ></span>
    <span v-if="label" class="text-sm text-text-secondary">{{ label }}</span>
  </span>
</template>
