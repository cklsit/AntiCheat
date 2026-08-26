import type { ApiResp, DashboardStats, AlertItem } from '@/types'
import { RiskLevel } from '@/types'
import { delay } from '@/utils/mockDelay'
import { request } from './request'

/** 生产环境走真实 /api，开发环境走 mock */
const USE_REAL = import.meta.env.PROD

/**
 * Mock Dashboard 数据
 */
export async function getDashboardStats(): Promise<ApiResp<DashboardStats>> {
  if (USE_REAL) {
    const data = await request.get<DashboardStats>('/dashboard/stats')
    return { code: 0, message: 'ok', data }
  }
  await delay()
  return {
    code: 0,
    message: 'ok',
    data: {
      onlinePlayers: 1847,
      totalPlayers: 52318,
      activeCases: 14,
      todayViolations: 1284,
      todayBans: 27,
      riskDistribution: [
        { level: RiskLevel.LOW,     count: 48320, percent: 92.3 },
        { level: RiskLevel.MEDIUM,  count: 2975,  percent: 5.7 },
        { level: RiskLevel.HIGH,    count: 782,   percent: 1.5 },
        { level: RiskLevel.EXTREME, count: 241,   percent: 0.5 }
      ],
      moduleTriggers: [
        { module: 'KillAura',   count: 312, trend:  8.2 },
        { module: '移动检测',    count: 286, trend: -3.1 },
        { module: '速度',        count: 231, trend:  1.7 },
        { module: 'Scaffold',    count: 184, trend:  4.5 },
        { module: '飞行',        count: 129, trend: -1.2 },
        { module: 'Reach',       count: 97,  trend:  0.4 },
        { module: 'FastBreak',   count: 45,  trend: -2.5 }
      ],
      hourlyTrend: Array.from({ length: 24 }, (_, i) => ({
        hour: `${i.toString().padStart(2, '0')}:00`,
        violations: Math.round(30 + Math.random() * 90 + (i >= 18 && i <= 23 ? 50 : 0)),
        bans: Math.round(0.5 + Math.random() * 3 + (i >= 20 ? 1.5 : 0))
      })),
      serverStatus: [
        { id: 's1', name: 'survival-01', online: 320, tps: 19.8, memory: 62, region: '华东', status: 'healthy' },
        { id: 's2', name: 'survival-02', online: 285, tps: 19.5, memory: 67, region: '华南', status: 'healthy' },
        { id: 's3', name: 'skyblock-01', online: 210, tps: 18.2, memory: 72, region: '华东', status: 'healthy' },
        { id: 's4', name: 'skyblock-02', online: 188, tps: 16.4, memory: 78, region: '华北', status: 'warning' },
        { id: 's5', name: 'bedwars-01',  online: 345, tps: 19.9, memory: 58, region: '华东', status: 'healthy' },
        { id: 's6', name: 'bedwars-02',  online: 266, tps: 19.7, memory: 64, region: '华南', status: 'healthy' },
        { id: 's7', name: 'pvp-01',      online: 120, tps: 19.6, memory: 55, region: '华东', status: 'healthy' },
        { id: 's8', name: 'pvp-02',      online: 113, tps: 12.8, memory: 88, region: '华北', status: 'critical' }
      ]
    }
  }
}

/**
 * Mock 当前实时告警（Dashboard 侧栏）
 */
export async function getRecentAlerts(): Promise<ApiResp<AlertItem[]>> {
  if (USE_REAL) {
    const data = await request.get<AlertItem[]>('/dashboard/alerts')
    return { code: 0, message: 'ok', data }
  }
  await delay()
  return {
    code: 0,
    message: 'ok',
    data: [
      { id: 'al-1', level: 'critical', title: '自动封禁触发', message: '玩家 BoomerNA KillAura 95 > 阈值 92', playerName: 'BoomerNA', time: '2026-08-25T20:52:00', module: 'KillAura', score: 95 },
      { id: 'al-2', level: 'high',     title: '极高风险玩家在线', message: '玩家 Dream 持续高频 Scaffold 触发', playerName: 'Dream',    time: '2026-08-25T20:48:00', module: 'Scaffold', score: 87 },
      { id: 'al-3', level: 'high',     title: '硬件指纹关联', message: '检测到 3 个账号共用设备指纹', playerName: 'Herobrine', time: '2026-08-25T20:30:00', module: 'Association', score: 85 },
      { id: 'al-4', level: 'medium',   title: '中风险玩家进入', message: '玩家 Eret 进入 PVP 服务器', playerName: 'Eret',     time: '2026-08-25T20:28:00', module: '飞行', score: 71 },
      { id: 'al-5', level: 'medium',   title: 'TPS 异常波动', message: '服务器 pvp-02 TPS 跌落至 12.8', playerName: null as unknown as string, time: '2026-08-25T20:20:00', module: 'Monitor', score: 65 },
      { id: 'al-6', level: 'low',      title: '配置已更新',   message: 'KillAura 阈值由 90 调整为 92', playerName: null as unknown as string, time: '2026-08-25T20:55:12', module: 'Config', score: 0 }
    ]
  }
}
