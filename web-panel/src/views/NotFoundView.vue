<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ShieldAlert, Home, ArrowLeft } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
function goHome(): void { router.replace('/dashboard') }
</script>

<template>
  <div class="min-h-full w-full flex items-center justify-center p-5 bg-[radial-gradient(900px_500px_at_50%_-10%,rgba(0,229,255,0.10),transparent_60%)]">
    <div class="card max-w-md w-full text-center p-5 md:p-[40px]">
      <div class="mx-auto w-16 h-16 rounded-card flex items-center justify-center mb-4" style="background: rgba(0,229,255,0.12);">
        <ShieldAlert :size="32" style="color: var(--accent-cyan);"/>
      </div>
      <h1 class="m-0 mb-2 text-page-title text-text-primary">404 · 页面不存在</h1>
      <p class="text-body text-text-secondary mb-4">
        路径 <code class="font-mono px-1.5 py-0.5 rounded-tag text-caption" style="background: var(--bg-hover); color: var(--accent-cyan);">{{ route.fullPath }}</code>
        未在本面板中注册，可能已被迁移或暂时下线。
      </p>
      <div class="flex flex-wrap gap-2 justify-center">
        <button class="btn btn-secondary" @click="router.back()"><ArrowLeft :size="14"/>返回上一页</button>
        <button class="btn btn-primary" @click="goHome"><Home :size="14"/>回到总览</button>
      </div>
    </div>
  </div>
</template>
