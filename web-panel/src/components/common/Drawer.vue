<script setup lang="ts">
import { watch } from 'vue'

interface Props {
  visible: boolean
  title?: string
  side?: 'left' | 'right'
  width?: string
}

const props = withDefaults(defineProps<Props>(), {
  side: 'right',
  width: '480px'
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

function close(): void {
  emit('update:visible', false)
}

// ESC 关闭
watch(() => props.visible, (v) => {
  if (!v) return
  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault()
      close()
    }
  }
  window.addEventListener('keydown', onKey)
  const stopWatch = watch(() => props.visible, (nv) => {
    if (!nv) {
      window.removeEventListener('keydown', onKey)
      stopWatch()
    }
  })
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="visible"
        class="fixed inset-0 z-50"
        style="background: rgba(0,0,0,0.6);"
        @click.self="close"
      >
        <!-- 侧边 panel -->
        <Transition :name="side === 'right' ? 'slide-right' : 'slide-left'" appear>
          <div
            v-if="visible"
            class="absolute top-0 bottom-0 bg-bg-card border-border-line shadow-lg flex flex-col"
            :class="side === 'right' ? 'right-0 border-l' : 'left-0 border-r'"
            :style="{ width, maxWidth: '92vw' }"
            @click.stop
          >
            <!-- 头部 -->
            <div class="flex items-center justify-between px-4 py-3 border-b border-border-line shrink-0">
              <slot name="header">
                <h3 class="text-card-title text-text-primary m-0">{{ title || '' }}</h3>
              </slot>
              <button
                type="button"
                class="btn btn-ghost btn-icon !w-8 !h-8 !p-1"
                @click="close"
              >
                ×
              </button>
            </div>

            <!-- 内容 -->
            <div class="flex-1 overflow-y-auto px-4 py-3">
              <slot />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* 右侧抽屉：从右侧滑入 */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.28s ease;
}
.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}

/* 左侧抽屉 */
.slide-left-enter-active,
.slide-left-leave-active {
  transition: transform 0.28s ease;
}
.slide-left-enter-from,
.slide-left-leave-to {
  transform: translateX(-100%);
}
</style>
