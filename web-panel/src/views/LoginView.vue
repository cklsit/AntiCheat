<script setup lang="ts">
import { reactive, ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ShieldAlert, Eye, EyeOff, AlertCircle, Lock, User as UserIcon, ArrowRight, Loader2, KeyRound } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ username: 'admin', password: 'admin', totp: '', remember: true })
const showPassword = ref(false)
const submitting = ref(false)
const errorMsg = ref('')

const redirect = computed<string>(() => {
  const r = route.query.redirect
  return typeof r === 'string' && r.startsWith('/') ? r : '/dashboard'
})

const lockCountdown = ref(0)
let tickTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  tickTimer = setInterval(() => {
    lockCountdown.value = auth.remainingLockSeconds
  }, 500)
})

onBeforeUnmount(() => {
  if (tickTimer) clearInterval(tickTimer)
})

async function onSubmit(): Promise<void> {
  errorMsg.value = ''
  if (!form.username.trim() || !form.password) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  if (form.totp && !/^\d{6,8}$/.test(form.totp.trim())) {
    errorMsg.value = '动态验证码应为 6-8 位数字'
    return
  }
  if (auth.isLocked) {
    errorMsg.value = `登录尝试次数过多，请在 ${auth.remainingLockSeconds}s 后再试`
    return
  }
  submitting.value = true
  try {
    const res = await auth.login({
      username: form.username.trim(),
      password: form.password,
      totp: form.totp.trim() || undefined
    })
    if (res.ok) {
      router.replace(redirect.value).catch(() => void 0)
    } else {
      errorMsg.value = res.message
    }
  } finally {
    submitting.value = false
  }
}

function fillAccount(u: string, p: string): void {
  form.username = u
  form.password = p
  errorMsg.value = ''
}
</script>

<template>
  <div class="min-h-full w-full flex items-center justify-center p-4 bg-[radial-gradient(1200px_600px_at_10%_-10%,rgba(0,229,255,0.12),transparent_60%),radial-gradient(900px_500px_at_100%_110%,rgba(188,140,255,0.12),transparent_55%)]">
    <div class="w-full max-w-[980px] card shadow-lg overflow-hidden flex flex-col md:flex-row">
      <!-- 左侧品牌 -->
      <div class="md:w-[42%] p-5 md:p-[40px] bg-[linear-gradient(155deg,#12263a_0%,#0D1117_70%)] border-b md:border-b-0 md:border-r border-border-line flex flex-col justify-between">
        <div>
          <div class="flex items-center gap-3 mb-5">
            <div class="w-11 h-11 rounded-card bg-[linear-gradient(135deg,var(--accent-cyan),var(--accent-blue))] flex items-center justify-center">
              <ShieldAlert :size="26" color="#0D1117" stroke-width="2.3" />
            </div>
            <div>
              <div class="text-card-title text-text-primary">AntiCheat</div>
              <div class="text-caption text-text-secondary">Command Center</div>
            </div>
          </div>

          <h1 class="text-page-title !text-[28px] !font-semibold text-text-primary leading-snug mb-3">
            企业级反作弊<br/>指挥中心
          </h1>
          <p class="text-body text-text-secondary max-w-[360px] m-0">
            多模块融合检测 · 实时画像 · AI 判案 · 全链路审计。<br/>
            以毫秒级响应守护公平的游戏环境。
          </p>
        </div>

        <div class="mt-10 space-y-3">
          <div class="flex items-center gap-3 text-caption text-text-secondary">
            <span class="w-2 h-2 rounded-full" style="background: var(--success);"></span>
            KillAura / 移动 / 速度 / Scaffold / 飞行 / Reach 多模块
          </div>
          <div class="flex items-center gap-3 text-caption text-text-secondary">
            <span class="w-2 h-2 rounded-full" style="background: var(--accent-cyan);"></span>
            画像式风险评估 + 团队作弊关联挖掘
          </div>
          <div class="flex items-center gap-3 text-caption text-text-secondary">
            <span class="w-2 h-2 rounded-full" style="background: var(--accent-purple);"></span>
            贝叶斯融合 + 自适应学习闭环
          </div>
        </div>
      </div>

      <!-- 右侧表单 -->
      <div class="flex-1 p-5 md:p-[40px]">
        <h2 class="text-card-title !text-lg text-text-primary mb-1">欢迎回来</h2>
        <p class="text-caption text-text-secondary mb-4">登录以进入反作弊指挥中心。</p>

        <!-- 错误提示 -->
        <Transition name="fade">
          <div v-if="errorMsg" class="mb-4 p-3 rounded-btn border flex items-start gap-2" style="background: rgba(248,81,73,0.08); border-color: rgba(248,81,73,0.3);">
            <AlertCircle :size="16" style="color: var(--danger-red); margin-top: 1px; flex-shrink: 0;" />
            <span class="text-sm" style="color: var(--danger-red);">{{ errorMsg }}</span>
          </div>
        </Transition>
        <div v-if="auth.isLocked" class="mb-4 p-3 rounded-btn border flex items-start gap-2" style="background: rgba(210,153,34,0.08); border-color: rgba(210,153,34,0.3);">
          <Lock :size="16" style="color: var(--warning); margin-top: 1px; flex-shrink: 0;" />
          <span class="text-sm" style="color: var(--warning);">已临时锁定，剩余 {{ auth.remainingLockSeconds }}s（连续失败 5 次触发）</span>
        </div>

        <form class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <label class="input-label" for="username">用户名</label>
            <div class="relative">
              <UserIcon :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
              <input
                id="username" v-model="form.username" class="input pl-10" placeholder="请输入用户名"
                :disabled="submitting || auth.isLocked" autocomplete="username"
              />
            </div>
          </div>

          <div>
            <label class="input-label" for="password">密码</label>
            <div class="relative">
              <Lock :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
              <input
                id="password" v-model="form.password" class="input pl-10 pr-10"
                :type="showPassword ? 'text' : 'password'" placeholder="请输入密码"
                :disabled="submitting || auth.isLocked" autocomplete="current-password"
                @keyup.enter="onSubmit"
              />
              <button
                type="button" tabindex="-1"
                class="absolute right-2 top-1/2 -translate-y-1/2 btn btn-ghost btn-icon !w-8 !h-8 !p-0"
                @click="showPassword = !showPassword"
              >
                <component :is="showPassword ? EyeOff : Eye" :size="15" />
              </button>
            </div>
          </div>

          <!-- TOTP 动态验证码 -->
          <div>
            <label class="input-label" for="totp">动态验证码 (TOTP)</label>
            <div class="relative">
              <KeyRound :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
              <input
                id="totp" v-model="form.totp" class="input pl-10" placeholder="8 位数字（演示环境可留空）"
                inputmode="numeric" maxlength="8" autocomplete="one-time-code"
                :disabled="submitting || auth.isLocked"
              />
            </div>
          </div>

          <div class="flex items-center justify-between text-caption text-text-secondary">
            <label class="inline-flex items-center gap-2 cursor-pointer select-none">
              <input v-model="form.remember" type="checkbox" class="w-4 h-4" />
              <span>7 天内免登录</span>
            </label>
            <a class="hover:text-accent-blue cursor-pointer" style="color: var(--accent-blue);">忘记密码？</a>
          </div>

          <button
            type="submit" class="btn btn-primary w-full btn-lg"
            :disabled="submitting || auth.isLocked"
          >
            <Loader2 v-if="submitting" :size="16" class="animate-spin" />
            <span>{{ submitting ? '登录中…' : '登 录' }}</span>
            <ArrowRight v-if="!submitting" :size="16" />
          </button>

          <div class="pt-4">
            <div class="text-caption text-text-secondary mb-2">演示账号（用户名/密码一致）</div>
            <div class="flex flex-wrap gap-2">
              <button type="button" class="btn btn-sm btn-secondary" @click="fillAccount('admin','admin')">超级管理员 · admin</button>
              <button type="button" class="btn btn-sm btn-secondary" @click="fillAccount('mod','mod')">管理员 · mod</button>
              <button type="button" class="btn btn-sm btn-secondary" @click="fillAccount('reviewer','reviewer')">审理员 · reviewer</button>
              <button type="button" class="btn btn-sm btn-secondary" @click="fillAccount('observer','observer')">观察员 · observer</button>
            </div>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
