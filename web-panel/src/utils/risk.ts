import { RiskLevel } from '@/types'

/**
 * 根据分数返回风险等级枚举
 * 0-49 低 / 50-69 中 / 70-89 高 / 90-100 极高
 */
export function scoreToLevel(score: number): RiskLevel {
  const s = Math.max(0, Math.min(100, Math.round(score)))
  if (s >= 90) return RiskLevel.EXTREME
  if (s >= 70) return RiskLevel.HIGH
  if (s >= 50) return RiskLevel.MEDIUM
  return RiskLevel.LOW
}

/**
 * 返回对应风险分的十六进制颜色
 */
export function scoreToHexColor(score: number): string {
  const level = scoreToLevel(score)
  switch (level) {
    case RiskLevel.LOW:
      return '#3FB950'   // success green
    case RiskLevel.MEDIUM:
      return '#D29922'   // warning yellow
    case RiskLevel.HIGH:
      return '#FF6D00'   // danger orange
    case RiskLevel.EXTREME:
      return '#F85149'   // danger red
    default:
      return '#8B949E'
  }
}

/**
 * 风险等级 -> CSS class (用于 .tag-xxx 等)
 */
export function levelToCssClass(level: RiskLevel): string {
  switch (level) {
    case RiskLevel.LOW:
      return 'tag-green'
    case RiskLevel.MEDIUM:
      return 'tag-yellow'
    case RiskLevel.HIGH:
      return 'tag-orange'
    case RiskLevel.EXTREME:
      return 'tag-red'
    default:
      return 'tag-gray'
  }
}

/**
 * 风险等级 -> 中文名称
 */
export function levelToLabel(level: RiskLevel): string {
  switch (level) {
    case RiskLevel.LOW:     return '低风险'
    case RiskLevel.MEDIUM:  return '中风险'
    case RiskLevel.HIGH:    return '高风险'
    case RiskLevel.EXTREME: return '极高风险'
    default:                return '未知'
  }
}

/**
 * 玩家状态 -> 中文 + css 类
 */
export function playerStatusMeta(status: string): { label: string; cls: string; dot: string } {
  switch (status) {
    case 'online':   return { label: '在线', cls: 'tag-green', dot: 'online' }
    case 'offline':  return { label: '离线', cls: 'tag-gray', dot: 'offline' }
    case 'banned':   return { label: '已封禁', cls: 'tag-red', dot: 'banned' }
    case 'watching': return { label: '观察中', cls: 'tag-cyan', dot: 'watching' }
    default:         return { label: '未知', cls: 'tag-gray', dot: 'offline' }
  }
}

/**
 * 案件状态 -> 中文 + css 类
 */
export function caseStatusMeta(status: string): { label: string; cls: string; dot: string } {
  switch (status) {
    case 'pending':   return { label: '待处理', cls: 'tag-orange', dot: 'pending' }
    case 'reviewing': return { label: '审理中', cls: 'tag-blue', dot: 'reviewing' }
    case 'completed': return { label: '已完成', cls: 'tag-green', dot: 'completed' }
    default:          return { label: '未知', cls: 'tag-gray', dot: 'offline' }
  }
}
