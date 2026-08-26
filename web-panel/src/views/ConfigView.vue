<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  Settings2, ShieldAlert, Shield, Bell, Server, Database, Globe, Save, RotateCcw,
  ShieldCheck, Lock, Unlock, AlertTriangle
} from 'lucide-vue-next'
import { scoreToHexColor } from '@/utils/risk'

interface Threshold { name: string; module: string; autoBan: number; humanReview: number; enabled: boolean }

const thresholds = reactive<Threshold[]>([
  { name: '战斗 / KillAura', module: 'KillAura', autoBan: 92, humanReview: 70, enabled: true },
  { name: '移动 / 速度',   module: 'Speed',   autoBan: 95, humanReview: 75, enabled: true },
  { name: '移动 / 飞行',   module: 'Fly',     autoBan: 90, humanReview: 70, enabled: true },
  { name: '搭建 / Scaffold', module: 'Scaffold', autoBan: 88, humanReview: 65, enabled: true },
  { name: '交互 / Reach',   module: 'Reach',    autoBan: 85, humanReview: 60, enabled: true },
  { name: '挖掘 / FastBreak', module: 'FastBreak', autoBan: 90, humanReview: 70, enabled: false }
])

const system = reactive({
  serverName: 'AntiCheat Command Center',
  dataRetentionDays: 180,
  wsHeartbeat: 15,
  autoBanEnabled: true,
  reviewQueue: true,
  aiVerdictEnabled: true,
  notifyEmail: true,
  notifyWebhook: false,
  notifyWebhookUrl: 'https://hooks.example.com/xxxxx',
  dbHost: '10.0.2.50',
  dbPort: 3306,
  redisHost: '10.0.2.51',
  wsPort: 8080
})

const changes = ref(0)

function markDirty(): void { changes.value += 1 }
function reset(): void {
  // 简单提示
  changes.value = 0
}
function save(): void {
  // 模拟保存
  changes.value = 0
}
</script>

<template>
  <div class="space-y-4">
    <!-- 检测阈值 -->
    <div class="card">
      <div class="card-header">
        <h3 class="flex items-center gap-2"><ShieldAlert :size="16" style="color: var(--danger-red);"/> 模块检测阈值</h3>
        <div class="flex items-center gap-2 text-caption text-text-secondary">
          <span class="inline-flex items-center gap-1"><span class="w-2 h-2 rounded-full" style="background: var(--warning);"></span>人工审理</span>
          <span class="inline-flex items-center gap-1"><span class="w-2 h-2 rounded-full" style="background: var(--danger-red);"></span>自动封禁</span>
        </div>
      </div>
      <div class="card-body p-0">
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr><th style="width: 200px;">模块</th><th>启用</th><th>人工审理阈值</th><th>自动封禁阈值</th><th style="width: 220px;">区间可视化</th></tr>
            </thead>
            <tbody>
              <tr v-for="t in thresholds" :key="t.module" :class="{ 'opacity-50': !t.enabled }">
                <td>
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" class="w-4 h-4" v-model="t.enabled" @change="markDirty"/>
                    <span class="text-sm text-text-primary">{{ t.name }}</span>
                  </label>
                </td>
                <td><span class="tag" :class="t.enabled ? 'tag-green' : 'tag-gray'">{{ t.enabled ? '运行中' : '已停用' }}</span></td>
                <td>
                  <div class="flex items-center gap-3">
                    <input type="range" min="40" max="100" v-model.number="t.humanReview" :disabled="!t.enabled" class="w-40" @input="markDirty"/>
                    <span class="font-mono text-sm" style="color: var(--warning);">{{ t.humanReview }}</span>
                  </div>
                </td>
                <td>
                  <div class="flex items-center gap-3">
                    <input type="range" min="40" max="100" v-model.number="t.autoBan" :disabled="!t.enabled" class="w-40" @input="markDirty"/>
                    <span class="font-mono text-sm" style="color: var(--danger-red);">{{ t.autoBan }}</span>
                  </div>
                </td>
                <td>
                  <div class="relative h-4 w-full">
                    <div class="absolute inset-x-0 top-1/2 -translate-y-1/2 h-1.5 rounded-full bg-bg-hover overflow-hidden">
                      <div class="h-full rounded-full" style="background: linear-gradient(90deg, #3FB950 0%, #D29922 40%, #FF6D00 70%, #F85149 100%);"></div>
                    </div>
                    <div class="absolute top-1/2 -translate-y-1/2 w-3 h-3 rounded-full border-2 bg-bg-card" style="left: calc(var(--l) * 1%); --l: t.humanReview; border-color: var(--warning);"></div>
                    <div class="absolute top-1/2 -translate-y-1/2 w-3 h-3 rounded-full border-2 bg-bg-card" style="left: calc(var(--r) * 1%); --r: t.autoBan; border-color: var(--danger-red);"></div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <!-- 系统基础配置 -->
      <div class="card">
        <div class="card-header"><h3 class="flex items-center gap-2"><Settings2 :size="16" style="color: var(--accent-blue);"/> 系统基础设置</h3></div>
        <div class="card-body space-y-4">
          <div>
            <label class="input-label">服务器名称</label>
            <input v-model="system.serverName" class="input" @input="markDirty"/>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="input-label">数据保留 (天)</label>
              <input type="number" v-model.number="system.dataRetentionDays" class="input" @input="markDirty"/>
            </div>
            <div>
              <label class="input-label">WS 心跳 (秒)</label>
              <input type="number" v-model.number="system.wsHeartbeat" class="input" @input="markDirty"/>
            </div>
          </div>

          <div class="divider"></div>

          <label class="flex items-center justify-between py-1.5 cursor-pointer">
            <span class="flex items-center gap-2"><Lock :size="15" style="color: var(--danger-red);"/> <span class="text-sm text-text-primary">自动封禁 (超过阈值立即封禁)</span></span>
            <input type="checkbox" v-model="system.autoBanEnabled" class="w-4 h-4" @change="markDirty"/>
          </label>
          <label class="flex items-center justify-between py-1.5 cursor-pointer">
            <span class="flex items-center gap-2"><ShieldCheck :size="15" style="color: var(--accent-blue);"/> <span class="text-sm text-text-primary">开启审理队列 (人工复核模式)</span></span>
            <input type="checkbox" v-model="system.reviewQueue" class="w-4 h-4" @change="markDirty"/>
          </label>
          <label class="flex items-center justify-between py-1.5 cursor-pointer">
            <span class="flex items-center gap-2"><Sparkles :size="15" style="color: var(--accent-purple);"/> <span class="text-sm text-text-primary">AI 自动判决 (置信度 ≥ 0.92)</span></span>
            <input type="checkbox" v-model="system.aiVerdictEnabled" class="w-4 h-4" @change="markDirty"/>
          </label>
        </div>
      </div>

      <!-- 通知 & Webhook -->
      <div class="card">
        <div class="card-header"><h3 class="flex items-center gap-2"><Bell :size="16" style="color: var(--warning);"/> 通知渠道</h3></div>
        <div class="card-body space-y-4">
          <label class="flex items-center justify-between py-1.5 cursor-pointer">
            <span class="flex items-center gap-2"><Bell :size="15"/> <span class="text-sm text-text-primary">邮件通知 (高风险告警)</span></span>
            <input type="checkbox" v-model="system.notifyEmail" class="w-4 h-4" @change="markDirty"/>
          </label>
          <label class="flex items-center justify-between py-1.5 cursor-pointer">
            <span class="flex items-center gap-2"><Globe :size="15"/> <span class="text-sm text-text-primary">Webhook (Discord / Slack)</span></span>
            <input type="checkbox" v-model="system.notifyWebhook" class="w-4 h-4" @change="markDirty"/>
          </label>
          <div>
            <label class="input-label">Webhook URL</label>
            <input v-model="system.notifyWebhookUrl" class="input font-mono text-caption" :disabled="!system.notifyWebhook" @input="markDirty"/>
          </div>
          <div class="divider"></div>
          <div class="p-3 rounded-btn" style="background: rgba(63,185,80,0.08); border: 1px solid rgba(63,185,80,0.25);">
            <div class="flex items-start gap-2 mb-1">
              <ShieldCheck :size="15" style="color: var(--success); margin-top: 1px;"/>
              <span class="text-sm font-medium text-text-primary">配置安全</span>
            </div>
            <p class="text-caption text-text-secondary m-0">当前 TLS 端到端加密已启用，敏感字段存储已脱敏。</p>
          </div>
        </div>
      </div>

      <!-- 基础设施 -->
      <div class="card lg:col-span-2">
        <div class="card-header"><h3 class="flex items-center gap-2"><Database :size="16" style="color: var(--accent-cyan);"/> 基础设施</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="p-4 rounded-card border border-border-line bg-bg-hover space-y-3">
            <div class="flex items-center gap-2 text-text-primary"><Database :size="16" style="color: var(--accent-blue);"/><span class="font-medium">MySQL</span></div>
            <div class="grid grid-cols-2 gap-2 text-caption">
              <span class="text-text-secondary">Host</span><span class="font-mono text-text-primary">{{ system.dbHost }}</span>
              <span class="text-text-secondary">Port</span><span class="font-mono text-text-primary">{{ system.dbPort }}</span>
            </div>
            <div class="flex items-center gap-2 pt-1 text-caption">
              <span class="status-dot online" style="width:6px;height:6px;"></span>
              <span class="text-success">连接正常 · 延迟 2ms</span>
            </div>
          </div>
          <div class="p-4 rounded-card border border-border-line bg-bg-hover space-y-3">
            <div class="flex items-center gap-2 text-text-primary"><Server :size="16" style="color: var(--accent-purple);"/><span class="font-medium">Redis</span></div>
            <div class="grid grid-cols-2 gap-2 text-caption">
              <span class="text-text-secondary">Host</span><span class="font-mono text-text-primary">{{ system.redisHost }}</span>
              <span class="text-text-secondary">模式</span><span class="font-mono text-text-primary">Sentinel</span>
            </div>
            <div class="flex items-center gap-2 pt-1 text-caption">
              <span class="status-dot online" style="width:6px;height:6px;"></span>
              <span class="text-success">连接正常 · 命中率 98.4%</span>
            </div>
          </div>
          <div class="p-4 rounded-card border border-border-line bg-bg-hover space-y-3">
            <div class="flex items-center gap-2 text-text-primary"><Globe :size="16" style="color: var(--accent-cyan);"/><span class="font-medium">WebSocket</span></div>
            <div class="grid grid-cols-2 gap-2 text-caption">
              <span class="text-text-secondary">Port</span><span class="font-mono text-text-primary">{{ system.wsPort }}</span>
              <span class="text-text-secondary">心跳</span><span class="font-mono text-text-primary">{{ system.wsHeartbeat }}s</span>
            </div>
            <div class="flex items-center gap-2 pt-1 text-caption">
              <span class="status-dot watching" style="width:6px;height:6px;"></span>
              <span class="text-text-secondary">客户端 1,284 / 上限 10k</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部保存 -->
    <div class="sticky bottom-0 card">
      <div class="card-body flex flex-wrap items-center justify-between gap-3">
        <div class="text-caption text-text-secondary">
          <template v-if="changes > 0">
            <AlertTriangle :size="14" class="inline mr-1" style="color: var(--warning);"/>
            存在 <span class="font-mono text-warning">{{ changes }}</span> 处未保存的修改。
          </template>
          <template v-else>所有修改已保存。</template>
        </div>
        <div class="flex gap-2">
          <button class="btn btn-secondary" @click="reset"><RotateCcw :size="14"/> 重置</button>
          <button class="btn btn-primary" @click="save"><Save :size="14"/> 保存并生效</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Sparkles } from 'lucide-vue-next'
export default { components: { Sparkles } }
</script>
