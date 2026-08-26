<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { MonitorX } from 'lucide-vue-next'
import { useNotificationStore } from '@/stores/notification'

const MIN_WIDTH = 1280
const viewportWidth = ref<number>(typeof window === 'undefined' ? MIN_WIDTH : window.innerWidth)
const isSmallViewport = computed<boolean>(() => viewportWidth.value < MIN_WIDTH)

function onResize(): void {
  viewportWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener('resize', onResize)
  // 预加载通知
  useNotificationStore().fetchList().catch(() => void 0)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})
</script>

<template>
  <!-- 小屏警告层：<1280px 时显示，提示切换更大屏幕 -->
  <div v-if="isSmallViewport" class="small-screen-warning" role="alert">
    <MonitorX :size="64" :stroke-width="1.4" />
    <h2>请使用更大的屏幕</h2>
    <p>
      反作弊指挥中心当前仅支持宽度不低于 <strong>1280px</strong> 的桌面端浏览器。
      当前宽度为 <strong>{{ viewportWidth }}px</strong>，请放大窗口或使用分辨率更高的显示器访问，以获得完整的监控与操作体验。
    </p>
  </div>

  <!-- 正常宽度渲染主路由 -->
  <router-view v-else />
</template>

<style scoped>
/* 额外样式由 global.css 中的 .small-screen-warning 提供 */
</style>
