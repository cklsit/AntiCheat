<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Network, Users, ShieldAlert, Link2, Monitor, Shield } from 'lucide-vue-next'

interface Node {
  id: string
  label: string
  type: 'player' | 'hardware' | 'ip' | 'cluster'
  score: number
  x: number
  y: number
}

interface Edge { source: string; target: string; kind: 'hardware' | 'ip' | 'behavior'; weight: number }

const nodes: Node[] = [
  { id: 'n1',  label: 'Herobrine',  type: 'player',   score: 98, x: 50, y: 50 },
  { id: 'n2',  label: 'Hero_alt',   type: 'player',   score: 91, x: 68, y: 38 },
  { id: 'n3',  label: 'Player_X',   type: 'player',   score: 77, x: 70, y: 62 },
  { id: 'n4',  label: 'Dream',      type: 'player',   score: 87, x: 30, y: 30 },
  { id: 'n5',  label: 'xQc',        type: 'player',   score: 74, x: 22, y: 55 },
  { id: 'n6',  label: 'BoomerNA',   type: 'player',   score: 95, x: 80, y: 80 },
  { id: 'n7',  label: 'Jschlatt',   type: 'player',   score: 92, x: 18, y: 82 },
  { id: 'h1',  label: 'HWID-AA01',  type: 'hardware', score: 0,  x: 62, y: 50 },
  { id: 'h2',  label: 'HWID-BB03',  type: 'hardware', score: 0,  x: 36, y: 68 },
  { id: 'i1',  label: '45.33.11.22',type: 'ip',       score: 0,  x: 82, y: 48 },
  { id: 'i2',  label: '104.88.22.11',type: 'ip',      score: 0,  x: 85, y: 68 },
  { id: 'c1',  label: 'TEAM-RUSH',  type: 'cluster',  score: 0,  x: 50, y: 12 }
]

const edges: Edge[] = [
  { source: 'n1', target: 'h1', kind: 'hardware', weight: 0.99 },
  { source: 'n2', target: 'h1', kind: 'hardware', weight: 0.96 },
  { source: 'n3', target: 'h1', kind: 'hardware', weight: 0.72 },
  { source: 'n1', target: 'i1', kind: 'ip',       weight: 0.90 },
  { source: 'n2', target: 'i1', kind: 'ip',       weight: 0.88 },
  { source: 'n6', target: 'i2', kind: 'ip',       weight: 0.92 },
  { source: 'n5', target: 'h2', kind: 'hardware', weight: 0.65 },
  { source: 'n7', target: 'h2', kind: 'hardware', weight: 0.81 },
  { source: 'n1', target: 'c1', kind: 'behavior', weight: 0.85 },
  { source: 'n2', target: 'c1', kind: 'behavior', weight: 0.80 },
  { source: 'n4', target: 'c1', kind: 'behavior', weight: 0.72 }
]

const phase = ref(0)
let rafId = 0
onMounted(() => {
  const t = () => { phase.value += 0.01; rafId = requestAnimationFrame(t) }
  rafId = requestAnimationFrame(t)
})
onBeforeUnmount(() => cancelAnimationFrame(rafId))

function nodeColor(n: Node): string {
  if (n.type === 'hardware') return '#BC8CFF'
  if (n.type === 'ip')       return '#388BFD'
  if (n.type === 'cluster')  return '#00E5FF'
  if (n.score >= 90) return '#F85149'
  if (n.score >= 70) return '#FF6D00'
  if (n.score >= 50) return '#D29922'
  return '#3FB950'
}
function nodeRadius(n: Node): number {
  if (n.type === 'player')   return 7 + n.score / 14
  if (n.type === 'hardware') return 12
  if (n.type === 'ip')       return 10
  return 14
}
function edgeColor(e: Edge): string {
  return e.kind === 'hardware' ? '#BC8CFF' : e.kind === 'ip' ? '#388BFD' : '#00E5FF'
}
const NODE_TYPE_LABEL: Record<Node['type'], string> = {
  player: '玩家账号',
  hardware: '硬件指纹',
  ip: 'IP 地址',
  cluster: '团伙簇'
}
function nodeTypeLabel(n: Node): string {
  return NODE_TYPE_LABEL[n.type]
}
const NODE_TYPE_LABEL_SHORT: Record<Node['type'], string> = {
  player: '玩家',
  hardware: '硬件',
  ip: 'IP',
  cluster: '团伙'
}
function nodeTypeLabelShort(n: Node): string {
  return NODE_TYPE_LABEL_SHORT[n.type]
}
const selectedId = ref<string | null>(nodes[0].id)
const selected = ref<Node | null>(nodes[0])
function pick(id: string): void {
  selectedId.value = id
  selected.value = nodes.find((n) => n.id === id) || null
}

const relatedNodes = computed(() => {
  const n = selected.value
  if (!n) return [] as Node[]
  const ids = new Set<string>()
  edges.forEach((e) => { if (e.source === n.id) ids.add(e.target); if (e.target === n.id) ids.add(e.source) })
  return nodes.filter((x) => ids.has(x.id))
})
</script>

<template>
  <div class="space-y-4">
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <div class="stat-tile"><div>
        <div class="stat-tile-label">活跃团伙</div>
        <div class="stat-tile-value !text-[20px]">12</div>
        <div class="text-caption text-text-secondary">较昨日 +2</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(188,140,255,0.12); color: var(--accent-purple);"><Network :size="22"/></div></div>
      <div class="stat-tile"><div>
        <div class="stat-tile-label">监控账号</div>
        <div class="stat-tile-value !text-[20px]">47</div>
        <div class="text-caption text-text-secondary">共享指纹关联</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(0,229,255,0.12); color: var(--accent-cyan);"><Users :size="22"/></div></div>
      <div class="stat-tile"><div>
        <div class="stat-tile-label">极高风险节点</div>
        <div class="stat-tile-value !text-[20px]" style="color: var(--danger-red);">11</div>
        <div class="text-caption text-text-secondary">建议优先处置</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(248,81,73,0.12); color: var(--danger-red);"><ShieldAlert :size="22"/></div></div>
      <div class="stat-tile"><div>
        <div class="stat-tile-label">今日团伙打击</div>
        <div class="stat-tile-value !text-[20px]" style="color: var(--success);">3</div>
        <div class="text-caption text-text-secondary">封禁 8 个账号</div>
      </div><div class="stat-tile-icon-wrap" style="background: rgba(63,185,80,0.12); color: var(--success);"><Shield :size="22"/></div></div>
    </div>

    <div class="grid grid-cols-1 xl:grid-cols-[1fr_380px] gap-4">
      <div class="card">
        <div class="card-header">
          <h3 class="flex items-center gap-2"><Network :size="16" style="color: var(--accent-purple);"/> 联盟图谱</h3>
          <div class="flex items-center gap-3 text-caption text-text-secondary">
            <span class="inline-flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-full" style="background:#BC8CFF;"></span>硬件指纹</span>
            <span class="inline-flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-full" style="background:#388BFD;"></span>IP 地址</span>
            <span class="inline-flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-full" style="background:#00E5FF;"></span>行为相似</span>
          </div>
        </div>
        <div class="card-body p-0 relative overflow-hidden" style="height: 560px;">
          <!-- 背景 -->
          <div class="absolute inset-0" style="background:
            radial-gradient(700px 350px at 20% 10%, rgba(188,140,255,0.10), transparent 60%),
            radial-gradient(600px 300px at 90% 100%, rgba(0,229,255,0.10), transparent 55%);"></div>
          <div class="absolute inset-0"
               style="background-image:
                 radial-gradient(circle, rgba(48,54,61,0.4) 1px, transparent 1px);
                 background-size: 24px 24px;"></div>

          <svg viewBox="0 0 100 100" preserveAspectRatio="none" class="absolute inset-0 w-full h-full">
            <!-- 边 -->
            <g>
              <line
                v-for="(e, i) in edges" :key="'e'+i"
                :x1="(nodes.find((n)=>n.id===e.source)!).x"
                :y1="(nodes.find((n)=>n.id===e.source)!).y"
                :x2="(nodes.find((n)=>n.id===e.target)!).x"
                :y2="(nodes.find((n)=>n.id===e.target)!).y"
                :stroke="edgeColor(e)" :stroke-width="0.2 + e.weight * 0.4" stroke-opacity="0.7"
                stroke-dasharray="0.5, 0.5"
                :style="`animation: dash 2s linear infinite; animation-delay: -${i * 0.15}s;`"
              />
            </g>
            <!-- 节点 -->
            <g>
              <g v-for="n in nodes" :key="n.id" :transform="`translate(${n.x}, ${n.y})`"
                 :class="n.id === selectedId ? 'opacity-100' : 'opacity-90 hover:opacity-100'"
                 style="cursor: pointer; transform-box: fill-box;"
                 @click="pick(n.id)"
              >
                <!-- 脉动光环 -->
                <circle v-if="n.type === 'player' && n.score >= 80"
                        :r="nodeRadius(n) + (2 + Math.sin(phase * 3 + n.x) * 1.2)"
                        fill="none" :stroke="nodeColor(n)" stroke-opacity="0.35" stroke-width="0.3"/>
                <!-- 主圆 -->
                <circle :r="nodeRadius(n)" :fill="nodeColor(n)"
                        :stroke="n.id === selectedId ? '#fff' : 'rgba(13,17,23,0.9)'"
                        stroke-width="0.6" stroke-opacity="0.9"/>
                <text v-if="n.type !== 'player'" text-anchor="middle" dy="0.35"
                      font-size="2.2" fill="#0D1117" font-weight="700" font-family="JetBrains Mono, monospace">
                  {{ n.type === 'hardware' ? 'H' : n.type === 'ip' ? 'I' : 'T' }}
                </text>
              </g>
            </g>
          </svg>

          <!-- 节点标签 (HTML 叠加) -->
          <div
            v-for="n in nodes" :key="'label'+n.id"
            class="absolute -translate-x-1/2 -translate-y-1/2 pointer-events-none"
            :style="{ left: n.x + '%', top: n.y + '%' }"
          >
            <div class="text-[10px] font-mono px-1 rounded-tag whitespace-nowrap"
                 :style="{ transform: `translateY(${nodeRadius(n) * 3.8}px)`,
                          background: 'rgba(13,17,23,0.7)', color: nodeColor(n),
                          outline: '1px solid rgba(255,255,255,0.06)' }">
              {{ n.label }}{{ n.type === 'player' && n.score ? ' · ' + n.score : '' }}
            </div>
          </div>

          <!-- 左下统计 -->
          <div class="absolute bottom-4 left-4 card shadow-md !p-3 !rounded-tag text-caption space-y-1.5" style="min-width: 180px;">
            <div class="flex items-center gap-2 mb-1"><Monitor :size="14" style="color: var(--accent-cyan);"/> 图谱统计</div>
            <div class="flex justify-between"><span class="text-text-secondary">节点</span><span class="font-mono text-text-primary">{{ nodes.length }}</span></div>
            <div class="flex justify-between"><span class="text-text-secondary">边数</span><span class="font-mono text-text-primary">{{ edges.length }}</span></div>
            <div class="flex justify-between"><span class="text-text-secondary">平均密度</span><span class="font-mono text-text-primary">0.286</span></div>
            <div class="flex justify-between"><span class="text-text-secondary">连通分量</span><span class="font-mono text-text-primary">3</span></div>
          </div>
        </div>
      </div>

      <!-- 右侧详情 -->
      <div class="space-y-4">
        <div class="card">
          <div class="card-header"><h3 class="flex items-center gap-2"><Link2 :size="16" style="color: var(--accent-cyan);"/> 选中详情</h3></div>
          <div v-if="selected" class="card-body space-y-3">
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 rounded-card flex items-center justify-center text-white font-semibold shrink-0"
                   :style="{ background: nodeColor(selected as Node) }">
                {{ (selected as Node).label.charAt(0) }}
              </div>
              <div class="min-w-0 flex-1">
                <div class="text-card-title text-text-primary truncate">{{ (selected as Node).label }}</div>
                <div class="text-caption text-text-secondary">
                  {{ nodeTypeLabel(selected as Node) }}
                </div>
              </div>
              <span
                class="tag"
                :class="(selected as Node).score >= 90 ? 'tag-red' : (selected as Node).score >= 70 ? 'tag-orange' : (selected as Node).score >= 50 ? 'tag-yellow' : 'tag-green'"
              >{{ (selected as Node).type === 'player' ? (selected as Node).score + ' 分' : '支持点' }}</span>
            </div>

            <div class="divider"></div>

            <div>
              <div class="text-caption text-text-secondary mb-2">关联实体 ({{ relatedNodes.length }})</div>
              <div class="space-y-1.5">
                <div
                  v-for="r in relatedNodes" :key="r.id"
                  class="flex items-center gap-3 p-2 rounded-btn hover:bg-bg-hover cursor-pointer border border-transparent hover:border-border-line transition-colors"
                  @click="pick(r.id)"
                >
                  <div class="w-7 h-7 rounded-btn flex items-center justify-center text-[11px] font-semibold text-white shrink-0"
                       :style="{ background: nodeColor(r) }">{{ r.label.charAt(0) }}</div>
                  <div class="min-w-0 flex-1">
                    <div class="text-sm text-text-primary truncate">{{ r.label }}</div>
                    <div class="text-caption text-text-muted">
                      {{ nodeTypeLabelShort(r) }}
                      <template v-if="r.type === 'player'"> · 风险 {{ r.score }}</template>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="divider"></div>
            <div class="flex gap-2">
              <button class="btn btn-secondary flex-1">查看画像</button>
              <button class="btn btn-danger flex-1">联动封禁</button>
            </div>
          </div>
          <div v-else class="card-body text-center text-text-secondary text-sm">点击左侧节点查看详情</div>
        </div>

        <div class="card">
          <div class="card-header"><h3 class="flex items-center gap-2"><ShieldAlert :size="16" style="color: var(--danger-red);"/> 风险团伙 Top 5</h3></div>
          <div class="card-body p-0">
            <div class="divide-y divide-border-line">
              <div v-for="(t, idx) in [
                { name: 'TEAM-RUSH', members: 5, score: 96 },
                { name: 'CLUSTER-B12', members: 3, score: 89 },
                { name: 'CLUSTER-A03', members: 4, score: 84 },
                { name: 'CLUSTER-D07', members: 2, score: 78 },
                { name: 'CLUSTER-C01', members: 3, score: 65 }
              ]" :key="t.name" class="px-4 py-3 flex items-center gap-3 hover:bg-bg-hover cursor-pointer">
                <div class="w-7 h-7 rounded-btn flex items-center justify-center font-mono text-sm font-semibold"
                     :style="{ background: idx === 0 ? 'rgba(248,81,73,0.15)' : 'rgba(139,148,158,0.12)', color: idx === 0 ? 'var(--danger-red)' : 'var(--text-secondary)' }">
                  {{ idx + 1 }}
                </div>
                <div class="flex-1 min-w-0">
                  <div class="text-sm font-medium text-text-primary">{{ t.name }}</div>
                  <div class="text-caption text-text-secondary">{{ t.members }} 成员</div>
                </div>
                <div class="font-mono text-sm" :style="{ color: t.score >= 90 ? 'var(--danger-red)' : t.score >= 70 ? 'var(--danger-orange)' : 'var(--warning)'}">{{ t.score }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
@keyframes dash { to { stroke-dashoffset: -2 } }
</style>
