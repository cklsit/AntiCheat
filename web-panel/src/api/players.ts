import type { ApiResp, Page, Player } from '@/types'
import playersJson from './mock/players.json'
import { delay } from '@/utils/mockDelay'
import { scoreToLevel } from '@/utils/risk'
import { request } from './request'

/** 生产环境走真实 /api，开发环境走 mock */
const USE_REAL = import.meta.env.PROD

const PLAYERS: Player[] = (playersJson as Player[]).map((p) => ({
  ...p,
  riskLevel: scoreToLevel(p.riskScore)
}))

function pick<T>(arr: T[], page = 1, pageSize = 10): Page<T> {
  const total = arr.length
  const pages = Math.max(1, Math.ceil(total / pageSize))
  const start = (page - 1) * pageSize
  return {
    list: arr.slice(start, start + pageSize),
    total,
    page,
    pageSize,
    pages
  }
}

export interface PlayerListQuery {
  page?: number
  pageSize?: number
  keyword?: string
  status?: Player['status'] | ''
  minScore?: number
  maxScore?: number
}

export async function getPlayerList(q: PlayerListQuery = {}): Promise<ApiResp<Page<Player>>> {
  if (USE_REAL) {
    const data = await request.get<Page<Player>>('/players', { params: q })
    return { code: 0, message: 'ok', data }
  }
  await delay()
  let list = PLAYERS.slice()
  if (q.keyword) {
    const k = q.keyword.toLowerCase()
    list = list.filter((p) => p.name.toLowerCase().includes(k) || p.uuid.includes(k) || p.ip.includes(k))
  }
  if (q.status) {
    list = list.filter((p) => p.status === q.status)
  }
  if (typeof q.minScore === 'number') {
    list = list.filter((p) => p.riskScore >= q.minScore!)
  }
  if (typeof q.maxScore === 'number') {
    list = list.filter((p) => p.riskScore <= q.maxScore!)
  }
  list.sort((a, b) => b.riskScore - a.riskScore)
  return {
    code: 0,
    message: 'ok',
    data: pick(list, q.page || 1, q.pageSize || 10)
  }
}

export async function getPlayerDetail(uuid: string): Promise<ApiResp<Player>> {
  if (USE_REAL) {
    const data = await request.get<Player>(`/players/${encodeURIComponent(uuid)}`)
    return { code: 0, message: 'ok', data }
  }
  await delay()
  const p = PLAYERS.find((x) => x.uuid === uuid)
  if (!p) return { code: 404, message: '玩家不存在', data: undefined as never }
  return { code: 0, message: 'ok', data: p }
}

export async function banPlayer(uuid: string, payload: { duration: string; reason?: string }): Promise<ApiResp<null>> {
  if (USE_REAL) {
    await request.post<null>(`/players/${encodeURIComponent(uuid)}/ban`, payload)
    return { code: 0, message: 'ok', data: null }
  }
  await delay(300)
  return { code: 0, message: 'ok', data: null }
}
