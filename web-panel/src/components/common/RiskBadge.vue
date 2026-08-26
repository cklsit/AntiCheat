<script setup lang="ts">
import { computed } from 'vue'
import { scoreToLevel, scoreToHexColor, levelToLabel } from '@/utils/risk'

interface Props {
  score: number
  mode?: 'dot' | 'text' | 'pill'
  showLabel?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'text',
  showLabel: true
})

const color = computed(() => scoreToHexColor(props.score))
const level = computed(() => scoreToLevel(props.score))
const labelText = computed(() => levelToLabel(level.value))
</script>

<template>
  <!-- dot 模式：仅色点 -->
  <span v-if="mode === 'dot'" class="inline-block shrink-0">
    <span
      class="inline-block rounded-full"
      :style="{ width: '8px', height: '8px', background: color }"
    ></span>
  </span>

  <!-- pill 模式：胶囊型 -->
  <span
    v-else-if="mode === 'pill'"
    class="inline-flex items-center gap-2 px-3 py-1 rounded-tag border"
    :style="{
      background: `${color}1A`,
      borderColor: `${color}66`,
      color: color
    }"
  >
    <span
      class="inline-block rounded-full shrink-0"
      :style="{ width: '6px', height: '6px', background: color }"
    ></span>
    <span class="text-xs font-semibold">{{ Math.round(score) }}</span>
    <span v-if="showLabel" class="text-xs font-medium">{{ labelText }}</span>
  </span>

  <!-- text 模式：默认 [色条] 评分 + 标签 -->
  <span v-else class="inline-flex items-center gap-2 align-middle">
    <span
      class="inline-block shrink-0 rounded-tag"
      :style="{
        width: '36px',
        height: '6px',
        background: color
      }"
    ></span>
    <span class="text-sm font-semibold text-text-primary">{{ Math.round(score) }}</span>
    <span v-if="showLabel" class="text-xs text-text-secondary">{{ labelText }}</span>
  </span>
</template>
