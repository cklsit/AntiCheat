<script setup lang="ts">
import { computed } from 'vue'
import { scoreToLevel, scoreToHexColor, levelToLabel } from '@/utils/risk'

interface Props {
  score: number
  triggers?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  triggers: () => []
})

const color = computed(() => scoreToHexColor(props.score))
const levelLabel = computed(() => levelToLabel(scoreToLevel(props.score)))
</script>

<template>
  <div class="relative inline-flex items-center">
    <!-- 18×18 圆形指示器 -->
    <div
      class="shrink-0 rounded-full"
      :style="{
        width: '18px',
        height: '18px',
        background: `linear-gradient(135deg, ${color}, ${color}CC)`,
        boxShadow: `0 0 0 3px ${color}40`
      }"
    ></div>

    <!-- 悬浮弹层：纯 CSS :hover 实现 -->
    <div
      class="tooltip-popup absolute left-1/2 -translate-x-1/2 bottom-[calc(100%+10px)] opacity-0 invisible pointer-events-none
             transition-all duration-150 z-50 p-3 rounded-card border border-border-line bg-bg-hover shadow-md"
      :style="{ width: '240px' }"
    >
      <div class="text-sm font-semibold text-text-primary mb-2">风险等级：{{ levelLabel }}</div>
      <ul v-if="triggers && triggers.length > 0" class="list-none m-0 p-0 space-y-1">
        <li
          v-for="(t, idx) in triggers"
          :key="idx"
          class="text-xs text-text-secondary flex items-start gap-2"
        >
          <span class="shrink-0 text-accent-cyan mt-0.5">▸</span>
          <span>{{ t }}</span>
        </li>
      </ul>
      <div v-else class="text-xs text-text-muted">暂无触发因素</div>
      <!-- 小三角 -->
      <div
        class="absolute left-1/2 -translate-x-1/2 top-full w-0 h-0"
        :style="{
          borderLeft: '6px solid transparent',
          borderRight: '6px solid transparent',
          borderTop: '6px solid var(--bg-hover)'
        }"
      ></div>
    </div>
  </div>
</template>

<style scoped>
.relative:hover .tooltip-popup {
  opacity: 1;
  visibility: visible;
}
</style>
