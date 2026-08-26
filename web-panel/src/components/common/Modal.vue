<script setup lang="ts">
import { watch } from 'vue'

interface Props {
  visible: boolean
  title: string
  width?: string
  footer?: boolean
  closeOnMask?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  width: '480px',
  footer: true,
  closeOnMask: true
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: []
  cancel: []
}>()

function close(): void {
  emit('update:visible', false)
}

function handleMaskClick(): void {
  if (props.closeOnMask) {
    emit('cancel')
    close()
  }
}

function handleCancel(): void {
  emit('cancel')
  close()
}

function handleConfirm(): void {
  emit('confirm')
  close()
}

// ESC 关闭
watch(() => props.visible, (v) => {
  if (!v) return
  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault()
      handleCancel()
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
        class="fixed inset-0 z-50 flex items-center justify-center"
        style="background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); -webkit-backdrop-filter: blur(4px);"
        @click.self="handleMaskClick"
      >
        <Transition name="slide-up" appear>
          <div
            v-if="visible"
            class="rounded-modal border border-border-line bg-bg-card shadow-lg flex flex-col"
            :style="{ width, maxWidth: '92vw', maxHeight: '86vh' }"
            @click.stop
          >
            <!-- 头部 -->
            <div class="flex items-center justify-between px-4 py-3 border-b border-border-line shrink-0">
              <h3 class="text-card-title text-text-primary m-0">{{ title }}</h3>
              <button
                type="button"
                class="btn btn-ghost btn-icon !w-8 !h-8 !p-1"
                @click="handleCancel"
              >
                ×
              </button>
            </div>

            <!-- 内容 -->
            <div class="flex-1 overflow-y-auto px-4 py-3">
              <slot />
            </div>

            <!-- 底部 -->
            <div
              v-if="footer"
              class="flex items-center justify-end gap-2 px-4 py-3 border-t border-border-line shrink-0"
            >
              <slot name="footer">
                <button type="button" class="btn btn-secondary" @click="handleCancel">取消</button>
                <button type="button" class="btn btn-primary" @click="handleConfirm">确定</button>
              </slot>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
