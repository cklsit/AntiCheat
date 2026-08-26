import type { ApiResp, Page, CaseEntity } from '@/types'
import casesJson from './mock/cases.json'
import { delay } from '@/utils/mockDelay'
import { scoreToLevel } from '@/utils/risk'
import { request } from './request'

/** 生产环境走真实 /api，开发环境走 mock */
const USE_REAL = import.meta.env.PROD

const CASES: CaseEntity[] = (casesJson as CaseEntity[]).map((c) => ({
  ...c,
  riskLevel: typeof c.riskLevel === 'number' ? c.riskLevel : scoreToLevel(c.topScore)
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

export interface CaseListQuery {
  page?: number
  pageSize?: number
  status?: CaseEntity['status'] | ''
  keyword?: string
  minScore?: number
  maxScore?: number
}

export async function getCaseList(q: CaseListQuery = {}): Promise<ApiResp<Page<CaseEntity>>> {
  if (USE_REAL) {
    const data = await request.get<Page<CaseEntity>>('/cases', { params: q })
    return { code: 0, message: 'ok', data }
  }
  await delay()
  let list = CASES.slice()
  if (q.keyword) {
    const k = q.keyword.toLowerCase()
    list = list.filter(
      (c) =>
        c.id.toLowerCase().includes(k) ||
        c.playerName.toLowerCase().includes(k) ||
        c.topModule.toLowerCase().includes(k)
    )
  }
  if (q.status) {
    list = list.filter((c) => c.status === q.status)
  }
  if (typeof q.minScore === 'number') list = list.filter((c) => c.topScore >= q.minScore!)
  if (typeof q.maxScore === 'number') list = list.filter((c) => c.topScore <= q.maxScore!)
  // 待处理优先，然后按分数倒序
  const orderWeight = (s: CaseEntity['status']) => (s === 'pending' ? 0 : s === 'reviewing' ? 1 : 2)
  list.sort((a, b) => {
    const oa = orderWeight(a.status)
    const ob = orderWeight(b.status)
    if (oa !== ob) return oa - ob
    return b.topScore - a.topScore
  })
  return {
    code: 0,
    message: 'ok',
    data: pick(list, q.page || 1, q.pageSize || 10)
  }
}

export async function getCaseById(id: string): Promise<ApiResp<CaseEntity>> {
  if (USE_REAL) {
    const data = await request.get<CaseEntity>(`/cases/${encodeURIComponent(id)}`)
    return { code: 0, message: 'ok', data }
  }
  await delay()
  const c = CASES.find((x) => x.id === id)
  if (!c) return { code: 404, message: '案件不存在', data: undefined as never }
  return { code: 0, message: 'ok', data: c }
}

export async function submitVerdict(id: string, payload: { verdict: string; reason?: string }): Promise<ApiResp<null>> {
  if (USE_REAL) {
    await request.post<null>(`/cases/${encodeURIComponent(id)}/verdict`, payload)
    return { code: 0, message: 'ok', data: null }
  }
  await delay(300)
  return { code: 0, message: 'ok', data: null }
}
