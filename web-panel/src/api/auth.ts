import type { ApiResp, LoginPayload, LoginResult, UserInfo } from '@/types'
import { Role } from '@/types'
import { delay } from '@/utils/mockDelay'
import { request } from './request'

/** 生产环境走真实 /api，开发环境走 mock */
const USE_REAL = import.meta.env.PROD

/**
 * Mock 登录接口
 * - 接受 admin/admin (超级管理员)
 * - reviewer/reviewer (审理员), observer/observer (观察员), mod/mod (管理员)
 * - 其它账号: 账号/密码相同则成功(观察员)，否则返回错误
 */
const USER_DB: Record<string, { password: string; user: UserInfo }> = {
  admin: {
    password: 'admin',
    user: {
      id: 'u-admin',
      username: 'admin',
      nickname: '超级管理员',
      role: Role.SUPER_ADMIN,
      avatar: 'A',
      lastLogin: '2026-08-25T20:00:00',
      lastIp: '10.0.12.10',
      permissions: ['*']
    }
  },
  mod: {
    password: 'mod',
    user: {
      id: 'u-mod-anna',
      username: 'mod',
      nickname: 'Anna (管理员)',
      role: Role.ADMIN,
      avatar: 'M',
      lastLogin: '2026-08-25T10:10:00',
      lastIp: '10.0.12.33',
      permissions: ['player:read', 'player:ban', 'case:read', 'case:verdict', 'config:read']
    }
  },
  reviewer: {
    password: 'reviewer',
    user: {
      id: 'u-reviewer-alice',
      username: 'reviewer',
      nickname: 'Alice (审理员)',
      role: Role.REVIEWER,
      avatar: 'R',
      lastLogin: '2026-08-25T09:02:00',
      lastIp: '10.0.12.55',
      permissions: ['player:read', 'case:read', 'case:verdict']
    }
  },
  observer: {
    password: 'observer',
    user: {
      id: 'u-observer-tom',
      username: 'observer',
      nickname: 'Tom (观察员)',
      role: Role.OBSERVER,
      avatar: 'O',
      lastLogin: '2026-08-25T18:28:00',
      lastIp: '10.0.12.60',
      permissions: ['player:read', 'case:read', 'dashboard:read']
    }
  }
}

function genToken(username: string): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const payload = btoa(
    JSON.stringify({
      sub: username,
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 86400,
      nonce: Math.random().toString(36).slice(2, 10)
    })
  )
  const sig = btoa(username + '_' + Date.now())
  return `${header}.${payload}.${sig}`
}

export async function login(payload: LoginPayload): Promise<ApiResp<LoginResult>> {
  if (USE_REAL) {
    const data = await request.post<LoginResult>('/auth/login', payload)
    return { code: 0, message: 'ok', data }
  }
  await delay([300, 600])
  const entry = USER_DB[payload.username]
  let user: UserInfo
  if (entry) {
    if (entry.password !== payload.password) {
      return { code: 1001, message: '用户名或密码错误', data: undefined as never }
    }
    user = entry.user
  } else {
    // 通用 mock: 用户名/密码一致则以观察员身份登录
    if (payload.username !== payload.password || !payload.username) {
      return { code: 1001, message: '用户名或密码错误', data: undefined as never }
    }
    const first = payload.username.charAt(0).toUpperCase()
    user = {
      id: 'u-' + payload.username,
      username: payload.username,
      nickname: payload.username,
      role: Role.OBSERVER,
      avatar: first,
      lastLogin: new Date().toISOString().slice(0, 19),
      lastIp: '127.0.0.1',
      permissions: ['dashboard:read', 'player:read', 'case:read']
    }
  }
  return {
    code: 0,
    message: 'ok',
    data: { token: genToken(user.username), user }
  }
}

export async function logout(): Promise<ApiResp<null>> {
  if (USE_REAL) {
    await request.post<null>('/auth/logout')
    return { code: 0, message: 'ok', data: null }
  }
  await delay(200)
  return { code: 0, message: 'ok', data: null }
}

export async function getCurrentUser(): Promise<ApiResp<UserInfo>> {
  if (USE_REAL) {
    const data = await request.get<UserInfo>('/auth/me')
    return { code: 0, message: 'ok', data }
  }
  await delay(200)
  // 尝试从 localStorage 恢复
  try {
    const raw = localStorage.getItem('anticheat_user')
    if (raw) {
      const user = JSON.parse(raw) as UserInfo
      return { code: 0, message: 'ok', data: user }
    }
  } catch { /* noop */ }
  return { code: 401, message: '未登录', data: undefined as never }
}
