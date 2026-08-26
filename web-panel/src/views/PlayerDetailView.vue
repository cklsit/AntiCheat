<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Ban, Shield, ShieldAlert, Globe, Wifi, Clock, Monitor, Link2, AlertTriangle } from 'lucide-vue-next'
import type { Player } from '@/types'
import { getPlayerDetail } from '@/api/players'
import { formatDate, formatDuration, formatNumber, formatIp } from '@/utils/format'
import { playerStatusMeta, scoreToHexColor, levelToCssClass, levelToLabel } from '@/utils/risk'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const player = ref<Player | null>(null)
const notFound = ref(false)

onMounted(async () => {
  const id = route.params.id as string
  try {
    const resp = await getPlayerDetail(id)
    player.value = resp.data
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
})

function goBack(): void {
  router.back()
}
function goLinked(uuid: string): void {
  router.push(`/players/${uuid}`).catch(() => void 0)
}
const pl = computed(() => player.value as Player)
</script>

<template>
  <div v-if="loading" class="h-[50vh] flex items-center justify-center text-text-secondary text-sm">加载玩家画像中…</div>
  <div v-else-if="notFound" class="card">
    <div class="card-body p-10 text-center">
      <ShieldAlert :size="40" class="mx-auto mb-4" style="color: var(--danger-red);"/>
      <h3 class="text-card-title text-text-primary mb-2">未找到该玩家画像</h3>
      <p class="text-caption text-text-secondary mb-5">UUID 可能无效或数据已被清理。</p>
      <button class="btn btn-secondary" @click="router.replace('/players')">返回玩家列表</button>
    </div>
  </div>

  <div v-else class="space-y-4">
    <!-- 顶部返回 + 玩家卡 -->
    <div class="card">
      <div class="card-body p-5 flex flex-wrap gap-5">
        <button class="btn btn-secondary shrink-0" @click="goBack"><ArrowLeft :size="16"/>返回列表</button>

        <div class="flex items-center gap-4 flex-1 min-w-[300px]">
          <div class="w-16 h-16 rounded-card flex items-center justify-center text-2xl font-bold text-white shrink-0"
               style="background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));">
            {{ pl.avatar }}
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-3 mb-1">
              <h2 class="m-0 text-[20px] font-semibold text-text-primary leading-tight">{{ pl.name }}</h2>
              <span class="tag" :class="playerStatusMeta(pl.status).cls">
                <span class="status-dot" :class="playerStatusMeta(pl.status).dot" style="width:6px;height:6px;"></span>
                {{ playerStatusMeta(pl.status).label }}
              </span>
              <span class="tag" :class="levelToCssClass(pl.riskLevel)">{{ levelToLabel(pl.riskLevel) }} · {{ pl.riskScore }}</span>
            </div>
            <div class="text-caption text-text-secondary font-mono space-x-4">
              <span>UUID: {{ pl.uuid }}</span>
            </div>
            <div class="mt-2 flex flex-wrap gap-x-6 gap-y-1 text-caption text-text-secondary">
              <span class="inline-flex items-center gap-1.5"><Globe :size="13"/> 国家：{{ pl.country }}</span>
              <span class="inline-flex items-center gap-1.5"><Wifi :size="13"/> Ping：<span class="font-mono text-text-primary">{{ pl.ping }}ms</span></span>
              <span class="inline-flex items-center gap-1.5"><Monitor :size="13"/> 版本：{{ pl.version }}</span>
              <span class="inline-flex items-center gap-1.5"><Globe :size="13"/> 世界：{{ pl.world }}</span>
              <span class="inline-flex items-center gap-1.5">模式：{{ pl.gameMode }}</span>
            </div>
          </div>
        </div>

        <!-- 大数字指标 -->
        <div class="grid grid-cols-4 gap-4 w-full md:w-auto md:min-w-[480px]">
          <div class="text-center p-3 rounded-card border border-border-line bg-bg-hover">
            <div class="text-2xl font-semibold text-text-primary leading-none">{{ formatNumber(pl.violationsCount) }}</div>
            <div class="text-caption text-text-secondary mt-1.5">累计违规</div>
          </div>
          <div class="text-center p-3 rounded-card border border-border-line bg-bg-hover">
            <div class="text-2xl font-semibold leading-none font-mono" :style="{ color: scoreToHexColor(pl.riskScore) }">{{ pl.riskScore }}</div>
            <div class="text-caption text-text-secondary mt-1.5">当前风险分</div>
          </div>
          <div class="text-center p-3 rounded-card border border-border-line bg-bg-hover">
            <div class="text-2xl font-semibold text-text-primary leading-none font-mono">{{ formatIp(pl.ip, true) }}</div>
            <div class="text-caption text-text-secondary mt-1.5">最近 IP</div>
          </div>
          <div class="text-center p-3 rounded-card border border-border-line bg-bg-hover">
            <div class="text-2xl font-semibold text-text-primary leading-none">{{ formatDuration(pl.onlineDuration) }}</div>
            <div class="text-caption text-text-secondary mt-1.5">本次在线</div>
          </div>
        </div>
      </div>

      <div class="card-footer flex flex-wrap items-center justify-end gap-2 border-t border-border-line">
        <button class="btn btn-secondary"><Shield :size="14"/>观察模式</button>
        <button class="btn btn-outline-cyan">踢出玩家</button>
        <button class="btn btn-danger"><Ban :size="14"/>执行封禁</button>
      </div>
    </div>

    <div class="grid grid-cols-1 xl:grid-cols-3 gap-4">
      <!-- 违规历史 -->
      <div class="card xl:col-span-2">
        <div class="card-header"><h3 class="flex items-center gap-2"><AlertTriangle :size="16" style="color: var(--warning);"/> 违规历史</h3></div>
        <div class="card-body p-0">
          <div class="table-wrap">
            <table class="data-table">
              <thead><tr><th>时间</th><th>模块</th><th>类型</th><th>得分</th><th>服务器</th></tr></thead>
              <tbody>
                <tr v-if="pl.violationHistory.length === 0">
                  <td colspan="5" class="py-10 text-center text-text-secondary">暂无违规记录</td>
                </tr>
                <tr v-for="v in pl.violationHistory" :key="v.id">
                  <td class="font-mono text-caption text-text-secondary">{{ formatDate(v.time, 'date') }}</td>
                  <td class="text-text-primary">{{ v.module }}</td>
                  <td class="text-text-secondary">{{ v.type }}</td>
                  <td>
                    <div class="flex items-center gap-2">
                      <div class="risk-bar w-24"><div class="risk-bar-fill" :style="{ width: v.score + '%', background: scoreToHexColor(v.score) }"></div></div>
                      <span class="font-mono text-sm" :style="{ color: scoreToHexColor(v.score) }">{{ v.score }}</span>
                    </div>
                  </td>
                  <td class="text-caption text-text-secondary font-mono">{{ v.server }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 右侧：基础 + 关联账号 -->
      <div class="space-y-4">
        <div class="card">
          <div class="card-header"><h3 class="flex items-center gap-2"><Clock :size="16" style="color: var(--accent-cyan);"/> 时间线</h3></div>
          <div class="card-body space-y-3 text-sm">
            <div class="flex justify-between"><span class="text-text-secondary">首次进入</span><span class="font-mono text-text-primary">{{ pl.firstJoin }}</span></div>
            <div class="flex justify-between"><span class="text-text-secondary">最近登录</span><span class="font-mono text-text-primary">{{ formatDate(pl.lastJoin, 'full') }}</span></div>
            <div class="flex justify-between"><span class="text-text-secondary">最近触发</span><span class="font-mono text-text-primary">{{ formatDate(pl.lastTrigger, 'relative') }}</span></div>
            <div v-if="pl.hardwareId" class="flex justify-between"><span class="text-text-secondary">硬件指纹</span><span class="font-mono text-text-primary">{{ pl.hardwareId.slice(0, 16) }}…</span></div>
          </div>
        </div>

        <div class="card">
          <div class="card-header"><h3 class="flex items-center gap-2"><Link2 :size="16" style="color: var(--accent-purple);"/> 关联账号 ({{ pl.linkedAccounts.length }})</h3></div>
          <div class="card-body p-0">
            <div v-if="pl.linkedAccounts.length === 0" class="p-6 text-center text-text-secondary text-sm">未检测到关联账号</div>
            <div
              v-for="la in pl.linkedAccounts" :key="la.uuid"
              class="px-4 py-3 border-b last:border-b-0 border-border-line flex items-center gap-3 cursor-pointer hover:bg-bg-hover"
              @click="goLinked(la.uuid)"
            >
              <div class="avatar avatar-sm">{{ la.name.charAt(0) }}</div>
              <div class="min-w-0 flex-1">
                <div class="text-sm text-text-primary truncate">{{ la.name }}</div>
                <div class="text-caption text-text-secondary">{{ la.relation }}</div>
              </div>
              <div class="flex flex-col items-end gap-1">
                <span v-if="la.ipMatch" class="tag tag-red text-[10px]">IP 匹配</span>
                <span v-if="la.hardwareMatch" class="tag tag-orange text-[10px]">硬件匹配</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
