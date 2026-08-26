<script setup lang="ts">
import { ref, onMounted, computed, h } from 'vue'
import VChart from 'vue-echarts'
import {
  Brain, Sparkles, Cpu, LineChart, Target, ShieldCheck, AlertTriangle,
  Lightbulb, BookOpen, Activity
} from 'lucide-vue-next'

const running = ref(false)
const epoch = ref(3241)
const loss = ref(0.0412)
const acc = ref(0.9782)
const lastReport = ref('最近模型稳定，误报率 0.73%，漏报率 1.18%')

let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  timer = setInterval(() => {
    if (!running.value) return
    epoch.value += 1
    loss.value = Math.max(0.005, loss.value * (0.97 + Math.random() * 0.04))
    acc.value = Math.min(0.9995, acc.value + Math.random() * 0.001)
  }, 900)
})

function toggleTraining(): void { running.value = !running.value }

const curveOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis', backgroundColor: '#161B22', borderColor: '#30363D', textStyle: { color: '#E6EDF3', fontSize: 12 } },
  legend: { textStyle: { color: '#8B949E' }, right: 10, top: 4 },
  grid: { left: 40, right: 20, top: 36, bottom: 28 },
  xAxis: { type: 'category', data: Array.from({ length: 20 }, (_, i) => (i * 50).toString()), axisLabel: { color: '#8B949E' }, axisLine: { lineStyle: { color: '#30363D' } } },
  yAxis: { type: 'value', axisLabel: { color: '#8B949E' }, splitLine: { lineStyle: { color: '#1C2333' } } },
  series: [
    { name: 'Train Loss', type: 'line', smooth: true, symbol: 'none', lineStyle: { width: 2, color: '#BC8CFF' }, data: [0.41,0.35,0.30,0.26,0.21,0.19,0.17,0.14,0.12,0.11,0.10,0.09,0.08,0.07,0.068,0.060,0.055,0.050,0.044,0.041] },
    { name: 'Val Loss',   type: 'line', smooth: true, symbol: 'none', lineStyle: { width: 2, color: '#00E5FF' }, data: [0.43,0.37,0.32,0.28,0.23,0.21,0.19,0.16,0.14,0.13,0.12,0.11,0.097,0.086,0.077,0.069,0.062,0.057,0.052,0.047] }
  ]
}))

const confusionOption = computed(() => {
  const map = ['有罪', '无罪']
  const data = [[986, 14], [22, 978]]
  const max = 986
  return {
    backgroundColor: 'transparent',
    tooltip: { position: 'top', backgroundColor: '#161B22', borderColor: '#30363D', textStyle: { color: '#E6EDF3', fontSize: 12 } },
    grid: { left: 70, right: 40, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: ['预测:有罪','预测:无罪'], axisLabel: { color: '#E6EDF3' }, axisLine: { lineStyle: { color: '#30363D' } }, splitArea: { show: false } },
    yAxis: { type: 'category', data: ['实际:无罪','实际:有罪'], axisLabel: { color: '#E6EDF3' }, axisLine: { lineStyle: { color: '#30363D' } } },
    series: [{
      type: 'heatmap', data: [
        ['预测:有罪','实际:有罪',data[0][0]],
        ['预测:无罪','实际:有罪',data[0][1]],
        ['预测:有罪','实际:无罪',data[1][0]],
        ['预测:无罪','实际:无罪',data[1][1]]
      ],
      label: { show: true, color: '#0D1117', fontWeight: 600 },
      itemStyle: { borderColor: '#0D1117', borderWidth: 2, borderRadius: 4 },
      color: (p: unknown) => {
        const val = (p as { value: number[] }).value[2]
        const t = val / max
        const r = Math.round(16 + (248 - 16) * t)
        const g = Math.round(22 + (81 - 22) * (1 - Math.abs(0.5 - t) * 2))
        const b = Math.round(35 + (73 - 35) * (1 - t))
        return `rgb(${r},${g},${b})`
      }
    } as never]
  }
})
</script>

<template>
  <div class="space-y-4">
    <!-- 模型训练指标 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="stat-tile"><div>
        <div class="stat-tile-label">训练轮次 Epoch</div>
        <div class="stat-tile-value !text-[20px] font-mono">{{ epoch }}</div>
        <div class="text-caption text-text-secondary">{{ running ? '训练中…' : '已暂停' }}</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(188,140,255,0.12); color: var(--accent-purple);"><Brain :size="22"/></div></div>
      <div class="stat-tile"><div>
        <div class="stat-tile-label">损失值 Loss</div>
        <div class="stat-tile-value !text-[20px] font-mono">{{ loss.toFixed(4) }}</div>
        <div class="text-caption text-text-secondary">↓ {{ (loss * 0.03 * 100).toFixed(2) }}% / 步</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(248,81,73,0.12); color: var(--danger-red);"><Activity :size="22"/></div></div>
      <div class="stat-tile"><div>
        <div class="stat-tile-label">验证集 Accuracy</div>
        <div class="stat-tile-value !text-[20px] font-mono" style="color: var(--success);">{{ (acc * 100).toFixed(2) }}%</div>
        <div class="text-caption text-text-secondary">较上次 ↑ 0.18%</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(63,185,80,0.12); color: var(--success);"><Target :size="22"/></div></div>
      <div class="stat-tile"><div>
        <div class="stat-tile-label">模型版本</div>
        <div class="stat-tile-value !text-[18px] font-mono text-accent-blue">v2.3.1-stable</div>
        <div class="text-caption text-text-secondary">发布 2026-08-24</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(0,229,255,0.12); color: var(--accent-cyan);"><ShieldCheck :size="22"/></div></div>
    </div>

    <!-- 训练控制 + 图表 -->
    <div class="grid grid-cols-1 xl:grid-cols-3 gap-4">
      <div class="card xl:col-span-2">
        <div class="card-header">
          <h3 class="flex items-center gap-2"><LineChart :size="16" style="color: var(--accent-blue);"/> Loss 训练曲线</h3>
          <div class="flex items-center gap-2">
            <span class="tag tag-purple font-mono">BayesianFusion v2</span>
            <button :class="running ? 'btn btn-danger' : 'btn btn-primary'" class="btn-sm" @click="toggleTraining">
              <Sparkles :size="14"/>{{ running ? '停止训练' : '启动训练' }}
            </button>
          </div>
        </div>
        <div class="card-body" style="height: 300px;">
          <VChart :option="curveOption" autoresize style="width:100%;height:100%"/>
        </div>
      </div>

      <div class="card flex flex-col">
        <div class="card-header"><h3 class="flex items-center gap-2"><Cpu :size="16" style="color: var(--accent-cyan);"/> 模型参数</h3></div>
        <div class="card-body space-y-3 text-sm flex-1">
          <div class="flex justify-between"><span class="text-text-secondary">架构</span><span class="font-mono text-text-primary">FusionNet-B (Bayesian)</span></div>
          <div class="flex justify-between"><span class="text-text-secondary">参数量</span><span class="font-mono text-text-primary">14.2 M</span></div>
          <div class="flex justify-between"><span class="text-text-secondary">学习率</span><span class="font-mono text-text-primary">1e-4 (cosine)</span></div>
          <div class="flex justify-between"><span class="text-text-secondary">优化器</span><span class="font-mono text-text-primary">AdamW (weight-decay 1e-3)</span></div>
          <div class="flex justify-between"><span class="text-text-secondary">训练集大小</span><span class="font-mono text-text-primary">8.4 M samples</span></div>
          <div class="flex justify-between"><span class="text-text-secondary">设备</span><span class="tag tag-green">GPU A10 · 16GB</span></div>
          <div class="flex justify-between"><span class="text-text-secondary">推理耗时</span><span class="font-mono text-text-primary">0.82 ms / 样本</span></div>
          <div class="divider my-0"></div>
          <div class="p-3 rounded-btn" style="background: rgba(56,139,253,0.08); border: 1px solid rgba(56,139,253,0.3);">
            <div class="flex items-start gap-2 mb-1.5">
              <Lightbulb :size="16" style="color: var(--accent-blue); flex-shrink: 0; margin-top: 1px;"/>
              <span class="text-sm font-medium text-text-primary">AI 洞察</span>
            </div>
            <p class="text-caption text-text-secondary m-0 leading-relaxed">{{ lastReport }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 混淆矩阵 + 模型卡片 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div class="card">
        <div class="card-header">
          <h3 class="flex items-center gap-2"><AlertTriangle :size="16" style="color: var(--warning);"/> 混淆矩阵 (验证集, N=2000)</h3>
        </div>
        <div class="card-body" style="height: 320px;">
          <VChart :option="confusionOption" autoresize style="width:100%;height:100%"/>
        </div>
      </div>

      <div class="card flex flex-col">
        <div class="card-header"><h3 class="flex items-center gap-2"><BookOpen :size="16" style="color: var(--accent-purple);"/> 知识库 & 规则</h3></div>
        <div class="card-body p-0 flex-1 overflow-y-auto">
          <div class="divide-y divide-border-line">
            <div class="px-4 py-3 flex gap-3">
              <span class="w-8 h-8 rounded-btn bg-bg-hover text-accent-cyan flex items-center justify-center shrink-0 font-mono text-sm">R01</span>
              <div class="min-w-0">
                <div class="text-sm text-text-primary">Bayesian 先验概率阈值</div>
                <div class="text-caption text-text-secondary">P(guilty|evidence) ≥ 0.90 才触发自动封禁，否则提交人审。</div>
              </div>
            </div>
            <div class="px-4 py-3 flex gap-3">
              <span class="w-8 h-8 rounded-btn bg-bg-hover text-accent-blue flex items-center justify-center shrink-0 font-mono text-sm">R02</span>
              <div class="min-w-0">
                <div class="text-sm text-text-primary">多模块联合加权</div>
                <div class="text-caption text-text-secondary">KillAura × 1.4 / Scaffold × 1.2 / 飞行 × 1.6 / 其它 × 1.0</div>
              </div>
            </div>
            <div class="px-4 py-3 flex gap-3">
              <span class="w-8 h-8 rounded-btn bg-bg-hover text-warning flex items-center justify-center shrink-0 font-mono text-sm">R03</span>
              <div class="min-w-0">
                <div class="text-sm text-text-primary">申诉自动复审路径</div>
                <div class="text-caption text-text-secondary">申诉样本进入 replay buffer 并触发 re-train gate (loss↓ 生效)。</div>
              </div>
            </div>
            <div class="px-4 py-3 flex gap-3">
              <span class="w-8 h-8 rounded-btn bg-bg-hover text-danger-red flex items-center justify-center shrink-0 font-mono text-sm">R04</span>
              <div class="min-w-0">
                <div class="text-sm text-text-primary">关联团队检测</div>
                <div class="text-caption text-text-secondary">硬件/IP/行为相似度 ≥ 0.82 标记为同一作弊团伙。</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
