import type { ApiResp, Page, AuditItem } from '@/types'
import auditJson from './mock/audit.json'
import { delay } from '@/utils/mockDelay'
import { request } from './request'

/** 生产环境走真实 /api，开发环境走 mock */
const USE_REAL = import.meta.env.PROD

const AUDITS: AuditItem[] = auditJson as AuditItem[]

function pick<T>(arr: T[], page = 1, pageSize = 20): Page<T> {
  const total = arr.length
  const pages = Math.max(1, Math.ceil(total / pageSize))
  const start = (page - 1) * pageSize
  return { list: arr.slice(start, start + pageSize), total, page, pageSize, pages }
}

export interface AuditQuery {
  page?: number
  pageSize?: number
  type?: AuditItem['type'] | ''
  result?: AuditItem['result'] | ''
  keyword?: string
}

export async function getAuditList(q: AuditQuery = {}): Promise<ApiResp<Page<AuditItem>>> {
  if (USE_REAL) {
    const data = await request.get<Page<AuditItem>>('/audit', { params: q })
    return { code: 0, message: 'ok', data }
  }
  await delay()
  let list = AUDITS.slice()
  if (q.type) list = list.filter((a) => a.type === q.type)
  if (q.result) list = list.filter((a) => a.result === q.result)
  if (q.keyword) {
    const k = q.keyword.toLowerCase()
    list = list.filter(
      (a) =>
        a.operator.toLowerCase().includes(k) ||
        a.target.toLowerCase().includes(k) ||
        a.ip.includes(k) ||
        (a.detail || '').toLowerCase().includes(k)
    )
  }
  list.sort((a, b) => (a.time < b.time ? 1 : -1))
  return { code: 0, message: 'ok', data: pick(list, q.page || 1, q.pageSize || 20) }
}
