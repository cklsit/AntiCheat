/**
 * 格式化工具函数集
 */

/**
 * 格式化日期时间
 * @param input - 时间字符串、Date或毫秒时间戳
 * @param pattern - 'full'|'date'|'time'|'relative'
 */
export function formatDate(
  input: string | Date | number,
  pattern: 'full' | 'date' | 'time' | 'relative' = 'full'
): string {
  const d = input instanceof Date ? input : new Date(typeof input === 'number' ? input : input)
  if (Number.isNaN(d.getTime())) return '-'

  const pad = (n: number): string => n.toString().padStart(2, '0')
  const yyyy = d.getFullYear()
  const MM = pad(d.getMonth() + 1)
  const dd = pad(d.getDate())
  const HH = pad(d.getHours())
  const mm = pad(d.getMinutes())
  const ss = pad(d.getSeconds())

  if (pattern === 'date') return `${yyyy}-${MM}-${dd}`
  if (pattern === 'time') return `${HH}:${mm}:${ss}`
  if (pattern === 'relative') return relativeTime(d)
  return `${yyyy}-${MM}-${dd} ${HH}:${mm}:${ss}`
}

function relativeTime(d: Date): string {
  const diffMs = Date.now() - d.getTime()
  const diffSec = Math.floor(diffMs / 1000)
  if (diffSec < 60) return `${diffSec < 1 ? 1 : diffSec}秒前`
  const diffMin = Math.floor(diffSec / 60)
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 30) return `${diffDay}天前`
  const diffMonth = Math.floor(diffDay / 30)
  if (diffMonth < 12) return `${diffMonth}个月前`
  return `${Math.floor(diffMonth / 12)}年前`
}

/**
 * 格式化时长 (秒 -> "2h 31m" / "12s")
 */
export function formatDuration(totalSeconds: number): string {
  if (!totalSeconds || totalSeconds < 0) return '0s'
  totalSeconds = Math.floor(totalSeconds)
  const days = Math.floor(totalSeconds / 86400)
  const hours = Math.floor((totalSeconds % 86400) / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  const parts: string[] = []
  if (days > 0) parts.push(`${days}d`)
  if (hours > 0) parts.push(`${hours}h`)
  if (minutes > 0) parts.push(`${minutes}m`)
  if (seconds > 0 && parts.length < 2) parts.push(`${seconds}s`)
  return parts.length > 0 ? parts.join(' ') : '0s'
}

/**
 * 格式化数字 (千分位 + 紧凑 K/M)
 */
export function formatNumber(n: number, compact = false): string {
  if (n === null || n === undefined || Number.isNaN(n)) return '0'
  if (!compact) return n.toLocaleString('en-US')
  if (n < 1000) return n.toString()
  if (n < 1_000_000) return (n / 1000).toFixed(n < 10_000 ? 1 : 0).replace(/\.0$/, '') + 'K'
  if (n < 1_000_000_000) return (n / 1_000_000).toFixed(n < 10_000_000 ? 1 : 0).replace(/\.0$/, '') + 'M'
  return (n / 1_000_000_000).toFixed(1).replace(/\.0$/, '') + 'B'
}

/**
 * 格式化 IP (IPv4 打码部分显示)
 */
export function formatIp(ip: string, mask = false): string {
  if (!ip) return '-'
  if (!mask) return ip
  const parts = ip.split('.')
  if (parts.length === 4) {
    return `${parts[0]}.${parts[1]}.*.*`
  }
  const v6Parts = ip.split(':')
  if (v6Parts.length >= 4) {
    return `${v6Parts[0]}:${v6Parts[1]}:**:**`
  }
  return ip
}
