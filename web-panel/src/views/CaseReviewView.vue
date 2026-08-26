<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft, Ban, Shield, UserCheck, UserX, Eye, FileText, Download,
  PlaySquare, AlertTriangle, BarChart3, ShieldAlert
} from 'lucide-vue-next'
import type { CaseEntity } from '@/types'
import { getCaseById } from '@/api/cases'
import { formatNumber, formatDate } from '@/utils/format'
import { caseStatusMeta, scoreToHexColor, levelToCssClass, levelToLabel } from '@/utils/risk'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const caseEntity = ref<CaseEntity | null>(null)
const notFound = ref(false)

onMounted(async () => {
  try {
    const resp = await getCaseById(route.params.id as string)
    caseEntity.value = resp.data
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
})

function back(): void { router.back() }
const ce = computed(() => caseEntity.value as CaseEntity)
</script>

<template>
  <div v-if="loading" class="h-[50vh] flex items-center justify-center text-text-secondary text-sm">加载案件中…</div>
  <div v-else-if="notFound" class="card">
    <div class="card-body p-10 text-center">
      <ShieldAlert :size="40" class="mx-auto mb-4" style="color: var(--danger-red);"/>
      <h3 class="text-card-title mb-2">未找到该案件</h3>
      <button class="btn btn-secondary mt-4" @click="router.replace('/cases')">返回案件列表</button>
    </div>
  </div>

  <div v-else class="space-y-4">
    <!-- 顶部头 -->
    <div class="card">
      <div class="card-body p-5 flex flex-wrap gap-4 items-start">
        <button class="btn btn-secondary shrink-0" @click="back"><ArrowLeft :size="16"/>返回</button>
        <div class="flex-1 min-w-0">
          <div class="flex flex-wrap items-center gap-3 mb-2">
            <h2 class="m-0 text-[20px] font-semibold text-text-primary leading-tight">
              案件 <span class="font-mono text-accent-blue">{{ ce.id }}</span>
              <span class="mx-2 text-text-muted">·</span>
              玩家 {{ ce.playerName }}
            </h2>
            <span class="tag" :class="caseStatusMeta(ce.status).cls">
              <span class="status-dot" :class="caseStatusMeta(ce.status).dot" style="width:6px;height:6px;"></span>
              {{ caseStatusMeta(ce.status).label }}
            </span>
            <span class="tag" :class="levelToCssClass(ce.riskLevel)">{{ levelToLabel(ce.riskLevel) }}</span>
            <span v-if="ce.verdict" class="tag"
              :class="ce.verdict==='guilty' ? 'tag-red' : ce.verdict==='innocent' ? 'tag-green' : 'tag-cyan'">
              {{ ce.verdict === 'guilty' ? '已判有罪' : ce.verdict === 'innocent' ? '判定无罪' : '已观察' }}
            </span>
          </div>
          <div class="flex flex-wrap gap-x-5 gap-y-1 text-caption text-text-secondary">
            <span class="font-mono">创建: {{ formatDate(ce.createdAt, 'full') }}</span>
            <span class="font-mono">{{ ce.age < 24 ? ce.age + 'h ago' : Math.floor(ce.age/24) + 'd ago' }}</span>
            <span v-if="ce.assignedTo">指派: @{{ ce.assignedTo }}</span>
          </div>
        </div>
        <div class="flex flex-wrap gap-2 justify-end">
          <button class="btn btn-secondary"><FileText :size="14"/>生成报告</button>
          <button class="btn btn-secondary"><Download :size="14"/>导出证据包</button>
          <button class="btn btn-outline-cyan"><PlaySquare :size="14"/>开始回放</button>
          <button class="btn btn-secondary" :disabled="ce.verdict==='innocent'"><UserCheck :size="14"/>标记无罪</button>
          <button class="btn btn-secondary" :disabled="ce.verdict==='watched'"><Eye :size="14"/>观察模式</button>
          <button class="btn btn-danger" :disabled="ce.verdict==='guilty'"><Ban :size="14"/>判为有罪 / 封禁</button>
        </div>
      </div>
    </div>

    <!-- 摘要统计 -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <div class="stat-tile"><div>
        <div class="stat-tile-label">最高模块</div>
        <div class="stat-tile-value !text-[20px]">{{ ce.topModule }}</div>
        <div class="text-caption text-text-secondary">峰值 {{ ce.topScore }}</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(0,229,255,0.12); color: var(--accent-cyan);"><AlertTriangle :size="22"/></div></div>
      <div class="stat-tile"><div>
        <div class="stat-tile-label">证据数量</div>
        <div class="stat-tile-value !text-[22px]">{{ ce.evidenceCount }}</div>
        <div class="text-caption text-text-secondary">条记录待复核</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(56,139,253,0.12); color: var(--accent-blue);"><BarChart3 :size="22"/></div></div>
      <div class="stat-tile"><div>
        <div class="stat-tile-label">综合风险</div>
        <div class="stat-tile-value !text-[22px]" :style="{ color: scoreToHexColor(ce.topScore) }">{{ ce.topScore }}</div>
        <div class="text-caption text-text-secondary">{{ levelToLabel(ce.riskLevel) }}</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(248,81,73,0.12); color: var(--danger-red);"><ShieldAlert :size="22"/></div></div>
      <div class="stat-tile"><div>
        <div class="stat-tile-label">案件年龄</div>
        <div class="stat-tile-value !text-[22px]">
          {{ ce.age < 24 ? ce.age + 'h' : Math.floor(ce.age/24) + 'd' }}
        </div>
        <div class="text-caption text-text-secondary">SLA: 24h 内办结</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(210,153,34,0.12); color: var(--warning);"><Shield :size="22"/></div></div>
    </div>

    <!-- 模块详情 + 证据 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div class="card">
        <div class="card-header"><h3>模块触发汇总</h3></div>
        <div class="card-body space-y-3">
          <div v-for="m in ce.modules" :key="m.name">
            <div class="flex items-center justify-between mb-1.5">
              <span class="text-sm text-text-primary">{{ m.name }}</span>
              <span class="text-caption text-text-secondary font-mono">
                {{ formatNumber(m.triggerCount) }} 次 · AVG {{ m.avgScore.toFixed(0) }} · Peak
                <span :style="{ color: scoreToHexColor(m.peakScore) }">{{ m.peakScore }}</span>
              </span>
            </div>
            <div class="risk-bar h-2"><div class="risk-bar-fill" :style="{ width: m.peakScore + '%', background: scoreToHexColor(m.peakScore) }"></div></div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><h3>证据摘要</h3></div>
        <div class="card-body p-0">
          <div class="table-wrap">
            <table class="data-table">
              <thead><tr><th>类型</th><th>数量</th><th>峰值分</th><th>最近时间</th></tr></thead>
              <tbody>
                <tr v-for="e in ce.evidenceSummary" :key="e.type">
                  <td class="text-text-primary">{{ e.type }}</td>
                  <td class="font-mono text-text-primary">{{ e.count }}</td>
                  <td class="font-mono" :style="{ color: scoreToHexColor(e.peakScore) }">{{ e.peakScore }}</td>
                  <td class="text-caption text-text-secondary font-mono">{{ formatDate(e.lastTime, 'relative') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 回放与协作占位 -->
    <div class="card">
      <div class="card-header">
        <h3 class="flex items-center gap-2"><PlaySquare :size="16" style="color: var(--accent-cyan);"/> 证据回放工作区</h3>
        <div class="flex gap-2">
          <button class="btn btn-sm btn-secondary">上一条</button>
          <button class="btn btn-sm btn-primary">▶ 播放</button>
          <button class="btn btn-sm btn-secondary">下一条</button>
        </div>
      </div>
      <div class="card-body">
        <div class="w-full h-[340px] rounded-card flex items-center justify-center text-text-secondary border border-dashed border-border-line"
             style="background: repeating-linear-gradient(135deg, rgba(255,255,255,0.02) 0 12px, transparent 12px 24px);">
          <div class="text-center">
            <PlaySquare :size="42" class="mx-auto mb-3 opacity-60"/>
            <div class="text-sm text-text-secondary">回放引擎已就绪 · 选择左侧证据条目开始查看</div>
            <div class="text-caption text-text-muted mt-1">支持 1x / 2x / 4x 倍速 + 三维视角切换</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
