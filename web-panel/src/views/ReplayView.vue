<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Play, Pause, SkipBack, SkipForward, Volume2, VolumeX, Maximize2, Download,
  Camera, Layers, Move3D, Grid3x3, User as UserIcon, ClipboardList
} from 'lucide-vue-next'
import casesJson from '@/api/mock/cases.json'
import type { CaseEntity } from '@/types'

const recentCases: CaseEntity[] = (casesJson as CaseEntity[]).slice(0, 6)

const playing = ref(false)
const muted = ref(false)
const t = ref(24)       // 当前秒
const total = ref(97)   // 总秒数
const speed = ref(1)    // 倍速
const layers = ref({ world: true, hitbox: true, motion: true, packets: false })

const cur = computed(() => `${String(Math.floor(t.value/60)).padStart(2,'0')}:${String(t.value%60).padStart(2,'0')}`)
const dur = computed(() => `${String(Math.floor(total.value/60)).padStart(2,'0')}:${String(total.value%60).padStart(2,'0')}`)
const progress = computed(() => Math.min(100, t.value / total.value * 100))

function seek(e: MouseEvent): void {
  const el = e.currentTarget as HTMLDivElement
  const r = el.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - r.left) / r.width))
  t.value = Math.round(total.value * ratio)
}

function toggle(): void { playing.value = !playing.value }
function back(): void { t.value = Math.max(0, t.value - 5) }
function fwd(): void  { t.value = Math.min(total.value, t.value + 5) }
function setSpeed(s: number): void { speed.value = s }

const LAYERS_LABEL: Record<string, string> = {
  world: '世界渲染',
  hitbox: '命中盒',
  motion: '运动轨迹',
  packets: '报文可视化'
}
function layerLabel(k: string | number | symbol): string {
  return LAYERS_LABEL[String(k)] || String(k)
}
</script>

<template>
  <div class="space-y-4">
    <div class="grid grid-cols-1 xl:grid-cols-[1fr_360px] gap-4">
      <!-- 播放器主区 -->
      <div class="card flex flex-col">
        <!-- 画布 -->
        <div class="relative card-body p-0 overflow-hidden" style="aspect-ratio: 16 / 9;">
          <!-- 背景地图 -->
          <div class="absolute inset-0"
               style="background:
                 radial-gradient(800px 400px at 30% 40%, rgba(0,229,255,0.08), transparent 60%),
                 linear-gradient(180deg, #0B1018 0%, #141C27 60%, #0B121B 100%);">
          </div>
          <div class="absolute inset-0"
               style="background-image:
                 linear-gradient(rgba(48,54,61,0.35) 1px, transparent 1px),
                 linear-gradient(90deg, rgba(48,54,61,0.35) 1px, transparent 1px);
                 background-size: 48px 48px;"></div>

          <!-- 世界结构占位 -->
          <div class="absolute left-[30%] top-[55%] -translate-x-1/2 -translate-y-1/2 w-[32%] h-[22%] rounded-card"
               style="background: repeating-linear-gradient(90deg, rgba(139,148,158,0.08) 0 24px, rgba(139,148,158,0.12) 24px 25px); border: 1px solid rgba(139,148,158,0.25);"></div>

          <!-- 玩家 A -->
          <div class="absolute left-[42%] top-[50%] -translate-x-1/2 -translate-y-1/2">
            <div class="relative">
              <div class="w-6 h-6 rounded-t-md" style="background: linear-gradient(180deg, #FF6D00 0%, #F85149 100%); transform: perspective(400px) rotateX(60deg);"></div>
              <div class="absolute -top-10 left-1/2 -translate-x-1/2 text-caption whitespace-nowrap px-2 py-0.5 rounded-tag bg-bg-card border border-danger-red text-danger-red">
                <span class="font-mono">Herobrine · 98</span>
              </div>
              <!-- 视线 -->
              <div class="absolute left-1/2 top-1/2 w-0 h-0" style="transform-origin: left top;">
                <div class="absolute w-[220px] h-[1px]" style="background: linear-gradient(90deg, rgba(248,81,73,0.8), transparent); transform: rotate(-15deg);"></div>
              </div>
            </div>
          </div>

          <!-- 玩家 B -->
          <div class="absolute left-[62%] top-[54%] -translate-x-1/2 -translate-y-1/2">
            <div class="w-5 h-5 rounded-t-md" style="background: linear-gradient(180deg, #00E5FF 0%, #388BFD 100%); transform: perspective(400px) rotateX(60deg);"></div>
            <div class="absolute -top-8 left-1/2 -translate-x-1/2 text-caption whitespace-nowrap px-2 py-0.5 rounded-tag bg-bg-card border border-border-line text-text-secondary font-mono">
              Victim_01
            </div>
          </div>

          <!-- 命中方块 -->
          <div class="absolute left-[54%] top-[50%] w-4 h-4 rounded-sm animate-pulse" style="border: 1px solid var(--danger-red); background: rgba(248,81,73,0.18);"></div>

          <!-- 左上 HUD -->
          <div class="absolute top-3 left-3 space-y-1.5">
            <span class="tag tag-red">REC · LIVE DEMO</span>
            <div class="text-caption text-text-secondary font-mono space-y-0.5 px-2 py-1.5 rounded-tag bg-bg-card/70 border border-border-line">
              <div>CASE-001 · KillAura</div>
              <div>server: pvp-02</div>
              <div>tick 982471 → 982500</div>
            </div>
          </div>

          <!-- 右上 HUD -->
          <div class="absolute top-3 right-3 flex flex-col items-end gap-1.5">
            <span class="tag tag-cyan font-mono">×{{ speed }} 倍速</span>
            <div class="flex flex-wrap gap-1.5 max-w-[260px] justify-end">
              <span v-if="layers.world"   class="tag tag-blue">世界层</span>
              <span v-if="layers.hitbox"  class="tag tag-orange">命中盒</span>
              <span v-if="layers.motion"  class="tag tag-purple">轨迹</span>
              <span v-if="layers.packets" class="tag tag-yellow">报文</span>
            </div>
          </div>

          <!-- 底部控制条 -->
          <div class="absolute bottom-0 left-0 right-0 p-3 pt-5" style="background: linear-gradient(180deg, transparent, rgba(0,0,0,0.55));"></div>
          <div class="absolute bottom-3 left-3 right-3 flex flex-col gap-2">
            <div class="h-1.5 rounded-full bg-bg-hover/80 cursor-pointer" @click="seek">
              <div class="h-full rounded-full" :style="{ width: progress + '%', background: 'linear-gradient(90deg, var(--accent-blue), var(--accent-cyan))' }"></div>
            </div>
            <div class="flex items-center gap-3">
              <button class="btn btn-ghost btn-icon" @click="back"><SkipBack :size="16"/></button>
              <button class="btn btn-primary btn-icon !w-10 !h-10" @click="toggle">
                <component :is="playing ? Pause : Play" :size="18"/>
              </button>
              <button class="btn btn-ghost btn-icon" @click="fwd"><SkipForward :size="16"/></button>
              <span class="font-mono text-caption text-text-secondary">{{ cur }} / {{ dur }}</span>

              <div class="ml-auto flex items-center gap-1.5">
                <div class="flex rounded-btn border border-border-line overflow-hidden">
                  <button v-for="s in [0.5, 1, 2, 4]" :key="s" class="px-2.5 h-8 text-caption transition-colors"
                    :class="speed === s ? 'bg-accent-blue/20 text-accent-blue' : 'text-text-secondary hover:bg-bg-hover'"
                    @click="setSpeed(s)">{{ s }}×</button>
                </div>
                <button class="btn btn-ghost btn-icon" @click="muted = !muted">
                  <component :is="muted ? VolumeX : Volume2" :size="16"/>
                </button>
                <button class="btn btn-ghost btn-icon"><Camera :size="16"/></button>
                <button class="btn btn-ghost btn-icon"><Download :size="16"/></button>
                <button class="btn btn-ghost btn-icon"><Maximize2 :size="16"/></button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 侧栏：证据列表 + 图层 -->
      <div class="space-y-4">
        <div class="card">
          <div class="card-header"><h3 class="flex items-center gap-2"><Layers :size="16" style="color: var(--accent-purple);"/>图层开关</h3></div>
          <div class="card-body space-y-2">
            <label v-for="(v, k) in layers" :key="String(k)" class="flex items-center justify-between py-1.5 cursor-pointer">
              <span class="text-sm text-text-primary capitalize">{{ layerLabel(k) }}</span>
              <input type="checkbox" class="w-4 h-4" v-model="layers[k as keyof typeof layers]" />
            </label>
            <div class="divider my-2"></div>
            <div class="flex items-center gap-2">
              <button class="btn btn-sm btn-ghost flex-1"><Move3D :size="14"/> 视角</button>
              <button class="btn btn-sm btn-ghost flex-1"><Grid3x3 :size="14"/> 栅格</button>
            </div>
          </div>
        </div>

        <div class="card flex flex-col">
          <div class="card-header"><h3 class="flex items-center gap-2"><ClipboardList :size="16" style="color: var(--warning);"/> 证据序列</h3></div>
          <div class="card-body p-0 max-h-[420px] overflow-y-auto">
            <div v-for="c in recentCases" :key="c.id"
                 class="px-4 py-3 border-b last:border-b-0 border-border-line hover:bg-bg-hover cursor-pointer flex items-start gap-3">
              <div class="mt-0.5 w-1.5 h-1.5 rounded-full shrink-0"
                   :style="{ background: c.topScore >= 90 ? 'var(--danger-red)' : c.topScore >= 70 ? 'var(--danger-orange)' : c.topScore >= 50 ? 'var(--warning)' : 'var(--accent-blue)' }"></div>
              <div class="min-w-0 flex-1">
                <div class="flex items-center justify-between gap-2 mb-1">
                  <span class="font-mono text-accent-blue text-sm">{{ c.id }}</span>
                  <span class="tag tag-blue text-[10px]">{{ c.topModule }}</span>
                </div>
                <div class="text-sm text-text-primary truncate">{{ c.playerName }} · {{ c.evidenceCount }} 条证据</div>
              </div>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-header"><h3 class="flex items-center gap-2"><UserIcon :size="16" style="color: var(--accent-cyan);"/> 参与实体</h3></div>
          <div class="card-body space-y-2 text-sm">
            <div class="flex items-center gap-3 p-2 rounded-btn bg-bg-hover">
              <div class="avatar avatar-sm" style="background: linear-gradient(135deg, #FF6D00, #F85149);">H</div>
              <div class="flex-1"><div class="text-text-primary">Herobrine <span class="tag tag-red ml-1 text-[10px]">嫌疑人</span></div><div class="text-caption text-text-muted">Ping 96ms · 得分 98</div></div>
            </div>
            <div class="flex items-center gap-3 p-2 rounded-btn hover:bg-bg-hover">
              <div class="avatar avatar-sm" style="background: linear-gradient(135deg, #00E5FF, #388BFD);">V</div>
              <div class="flex-1"><div class="text-text-primary">Victim_01 <span class="tag tag-green ml-1 text-[10px]">受害人</span></div><div class="text-caption text-text-muted">Ping 38ms</div></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
