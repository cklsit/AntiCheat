import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginPayload, UserInfo } from '@/types'
import { login as apiLogin, logout as apiLogout, getCurrentUser } from '@/api/auth'

const LS_TOKEN = 'anticheat_token'
const LS_USER = 'anticheat_user'
const LS_ATTEMPTS = 'anticheat_login_attempts'
const LS_LOCKED = 'anticheat_login_locked_until'

export const useAuthStore = defineStore('auth', () => {
  // ---------- state ----------
  const token = ref<string | null>(null)
  const user = ref<UserInfo | null>(null)
  const loginAttempts = ref<number>(0)
  const lockUntil = ref<number | null>(null)   // 时间戳 (ms)
  const lastLogin = ref<{ time: string; ip: string } | null>(null)
  const loading = ref(false)

  // ---------- hydrate from localStorage ----------
  try {
    token.value = localStorage.getItem(LS_TOKEN)
    const u = localStorage.getItem(LS_USER)
    if (u) user.value = JSON.parse(u)
    const a = localStorage.getItem(LS_ATTEMPTS)
    if (a) loginAttempts.value = Math.max(0, parseInt(a, 10) || 0)
    const l = localStorage.getItem(LS_LOCKED)
    if (l) {
      const t = parseInt(l, 10)
      lockUntil.value = Number.isFinite(t) && t > Date.now() ? t : null
    }
    if (!lockUntil.value) localStorage.removeItem(LS_LOCKED)
  } catch { /* noop */ }

  // ---------- getters ----------
  const isLoggedIn = computed<boolean>(() => !!token.value && !!user.value)

  const isLocked = computed<boolean>(() => {
    if (!lockUntil.value) return false
    if (lockUntil.value > Date.now()) return true
    lockUntil.value = null
    localStorage.removeItem(LS_LOCKED)
    return false
  })

  const remainingLockSeconds = computed<number>(() => {
    if (!lockUntil.value) return 0
    return Math.max(0, Math.ceil((lockUntil.value - Date.now()) / 1000))
  })

  const roleLabel = computed<string>(() => {
    if (!user.value) return ''
    switch (user.value.role) {
      case 0: return '超级管理员'
      case 1: return '管理员'
      case 2: return '审理员'
      case 3: return '观察员'
      default: return '未知角色'
    }
  })

  // ---------- actions ----------
  function persistToken(t: string | null): void {
    token.value = t
    if (t) localStorage.setItem(LS_TOKEN, t)
    else localStorage.removeItem(LS_TOKEN)
  }

  function persistUser(u: UserInfo | null): void {
    user.value = u
    if (u) {
      localStorage.setItem(LS_USER, JSON.stringify(u))
      lastLogin.value = { time: u.lastLogin, ip: u.lastIp }
    } else {
      localStorage.removeItem(LS_USER)
    }
  }

  function hasPerm(perm: string): boolean {
    if (!user.value) return false
    const perms = user.value.permissions
    if (perms.includes('*')) return true
    if (perms.includes(perm)) return true
    // 支持前缀通配: dashboard:* 命中 dashboard:read
    return perms.some(
      (p) => p.endsWith(':*') && perm.startsWith(p.slice(0, -1))
    )
  }

  function onLoginFailure(): void {
    loginAttempts.value += 1
    localStorage.setItem(LS_ATTEMPTS, String(loginAttempts.value))
    if (loginAttempts.value >= 5) {
      const ms = 10 * 60 * 1000 // 锁定 10 分钟
      lockUntil.value = Date.now() + ms
      localStorage.setItem(LS_LOCKED, String(lockUntil.value))
    }
  }

  function resetAttempts(): void {
    loginAttempts.value = 0
    lockUntil.value = null
    localStorage.removeItem(LS_ATTEMPTS)
    localStorage.removeItem(LS_LOCKED)
  }

  async function login(payload: LoginPayload): Promise<{ ok: boolean; message: string }> {
    if (isLocked.value) {
      return { ok: false, message: `账号已被临时锁定，请在 ${remainingLockSeconds.value}s 后重试` }
    }
    loading.value = true
    try {
      const resp = await apiLogin(payload)
      if (resp.code === 0 && resp.data) {
        persistToken(resp.data.token)
        persistUser(resp.data.user)
        resetAttempts()
        return { ok: true, message: '登录成功' }
      }
      onLoginFailure()
      return { ok: false, message: resp.message || '登录失败' }
    } catch (e: unknown) {
      onLoginFailure()
      return { ok: false, message: (e as Error)?.message || '登录失败' }
    } finally {
      loading.value = false
    }
  }

  async function logout(): Promise<void> {
    loading.value = true
    try {
      await apiLogout()
    } catch { /* noop */ } finally {
      persistToken(null)
      persistUser(null)
      resetAttempts()
      loading.value = false
    }
  }

  async function restoreSession(): Promise<boolean> {
    if (isLoggedIn.value) return true
    if (!token.value) return false
    try {
      const resp = await getCurrentUser()
      if (resp.code === 0 && resp.data) {
        persistUser(resp.data)
        return true
      }
    } catch { /* noop */ }
    persistToken(null)
    persistUser(null)
    return false
  }

  return {
    // state
    token,
    user,
    loginAttempts,
    lockUntil,
    lastLogin,
    loading,
    // getters
    isLoggedIn,
    isLocked,
    remainingLockSeconds,
    roleLabel,
    // actions
    login,
    logout,
    restoreSession,
    hasPerm,
    resetAttempts
  }
})
