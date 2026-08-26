<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Filter, ChevronLeft, ChevronRight, Eye, ArrowUpDown, Globe, Ban } from 'lucide-vue-next'
import type { Player, Page } from '@/types'
import { getPlayerList, type PlayerListQuery } from '@/api/players'
import { formatDuration, formatNumber, formatIp, formatDate } from '@/utils/format'
import { playerStatusMeta, scoreToHexColor, levelToCssClass, levelToLabel } from '@/utils/risk'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const page = reactive<Page<Player> & { list: Player[] }>({ list: [], total: 0, page: 1, pageSize: 10, pages: 0 })
const query = reactive<
  Required<Pick<PlayerListQuery, 'page' | 'pageSize'>>
  & Omit<PlayerListQuery, 'page' | 'pageSize' | 'keyword'>
  & { keyword: string }
>({
  page: 1, pageSize: 10, keyword: '', status: '', minScore: undefined, maxScore: undefined
})

const queryK = computed(() => route.query.k)

onMounted(async () => {
  if (typeof queryK.value === 'string' && queryK.value) {
    query.keyword = queryK.value
  }
  await refresh()
})

watch(() => [query.page, query.pageSize, query.status, query.keyword], () => refresh(), { deep: false })

async function refresh(): Promise<void> {
  loading.value = true
  try {
    const resp = await getPlayerList({
      page: query.page, pageSize: query.pageSize, status: query.status || undefined, keyword: query.keyword || undefined,
      minScore: query.minScore, maxScore: query.maxScore
    })
    page.list = resp.data.list
    page.total = resp.data.total
    page.page = resp.data.page
    page.pages = resp.data.pages
  } finally {
    loading.value = false
  }
}

function resetFilters(): void {
  query.keyword = ''
  query.status = ''
  query.minScore = undefined
  query.maxScore = undefined
  query.page = 1
}

function goDetail(uuid: string): void {
  router.push(`/players/${uuid}`).catch(() => void 0)
}

function pageBtnDisabled(delta: number): boolean {
  const target = page.page + delta
  return target < 1 || target > page.pages || loading.value
}
</script>

<template>
  <div class="space-y-4">
    <!-- 过滤栏 -->
    <div class="card">
      <div class="card-body flex flex-wrap items-center gap-3">
        <div class="relative flex-1 min-w-[260px]">
          <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted"/>
          <input v-model="query.keyword" class="input pl-10" placeholder="搜索玩家名 / UUID / IP…" @keyup.enter="query.page = 1" />
        </div>
        <div class="flex items-center gap-2">
          <Filter :size="16" class="text-text-secondary"/>
          <select v-model="query.status" class="input !w-[140px]" @change="query.page = 1">
            <option value="">全部状态</option>
            <option value="online">在线</option>
            <option value="offline">离线</option>
            <option value="watching">观察中</option>
            <option value="banned">已封禁</option>
          </select>
          <div class="flex items-center gap-2 text-caption text-text-secondary">
            <span>风险分</span>
            <input v-model.number="query.minScore" type="number" min="0" max="100" class="input !w-[80px]" placeholder="最小" @change="query.page = 1" />
            <span>-</span>
            <input v-model.number="query.maxScore" type="number" min="0" max="100" class="input !w-[80px]" placeholder="最大" @change="query.page = 1" />
          </div>
          <button class="btn btn-sm btn-secondary" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <!-- 列表卡 -->
    <div class="card">
      <div class="card-header">
        <h3>玩家列表 <span class="ml-2 text-caption text-text-muted">共 {{ formatNumber(page.total) }} 条</span></h3>
        <div class="flex items-center gap-2 text-caption text-text-secondary">
          <ArrowUpDown :size="14"/> 按风险分降序
        </div>
      </div>

      <div v-if="loading && page.list.length === 0" class="card-body">
        <div class="h-64 flex items-center justify-center text-text-secondary text-sm">加载中…</div>
      </div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 28%;">玩家</th>
              <th>状态</th>
              <th>风险分 <span class="text-text-muted font-normal">/ 等级</span></th>
              <th>违规次数</th>
              <th>在线时长</th>
              <th>IP</th>
              <th>最近触发</th>
              <th style="width: 80px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in page.list" :key="p.uuid" @click="goDetail(p.uuid)" class="cursor-pointer">
              <td>
                <div class="flex items-center gap-3">
                  <div class="avatar">{{ p.avatar }}</div>
                  <div class="min-w-0">
                    <div class="text-sm font-medium text-text-primary truncate">{{ p.name }}</div>
                    <div class="text-caption text-text-muted font-mono truncate">{{ p.country }} · {{ p.version }} · {{ p.world }}</div>
                  </div>
                </div>
              </td>
              <td>
                <span class="tag" :class="playerStatusMeta(p.status).cls">
                  <span class="status-dot" :class="playerStatusMeta(p.status).dot" style="width:6px;height:6px;"></span>
                  {{ playerStatusMeta(p.status).label }}
                </span>
              </td>
              <td>
                <div class="flex items-center gap-3">
                  <div class="w-28">
                    <div class="risk-bar">
                      <div class="risk-bar-fill" :style="{ width: p.riskScore + '%', background: scoreToHexColor(p.riskScore) }"></div>
                    </div>
                  </div>
                  <span class="font-mono text-sm" :style="{ color: scoreToHexColor(p.riskScore) }">{{ p.riskScore }}</span>
                  <span class="tag" :class="levelToCssClass(p.riskLevel)">{{ levelToLabel(p.riskLevel) }}</span>
                </div>
              </td>
              <td class="font-mono text-text-primary">{{ formatNumber(p.violationsCount) }}</td>
              <td class="font-mono text-text-secondary">
                <span v-if="p.status === 'offline' && p.onlineDuration === 0" class="text-text-muted">-</span>
                <template v-else>{{ formatDuration(p.onlineDuration) }}</template>
              </td>
              <td class="font-mono text-caption text-text-secondary">{{ formatIp(p.ip) }} <Globe :size="11" class="inline ml-1 text-text-muted"/></td>
              <td class="text-caption text-text-secondary font-mono">{{ formatDate(p.lastTrigger, 'relative') }}</td>
              <td @click.stop>
                <button class="btn btn-sm btn-secondary" @click="goDetail(p.uuid)">
                  <Eye :size="14"/>详情
                </button>
              </td>
            </tr>
            <tr v-if="page.list.length === 0">
              <td colspan="8" class="text-center py-16 text-text-secondary">
                没有符合条件的玩家 <button class="btn btn-sm btn-ghost ml-2" @click="resetFilters">清除筛选</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="card-footer">
        <div class="text-caption text-text-secondary">
          第 <span class="font-mono text-text-primary">{{ page.page }}</span> / <span class="font-mono">{{ page.pages || 1 }}</span> 页 · 每页
          <select v-model.number="query.pageSize" class="inline-block mx-1 input !w-[80px] !py-1 !h-8" @change="query.page = 1">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
          条
        </div>
        <div class="flex items-center gap-1">
          <button class="btn btn-sm btn-secondary" :disabled="pageBtnDisabled(-1)" @click="query.page -= 1"><ChevronLeft :size="14"/></button>
          <button
            v-for="n in Array.from({ length: Math.min(5, page.pages) }, (_, i) => {
              const start = Math.max(1, Math.min(page.page - 2, page.pages - 4))
              return start + i
            }).filter((n) => n >= 1 && n <= page.pages)"
            :key="n"
            class="btn btn-sm"
            :class="n === page.page ? 'btn-primary' : 'btn-secondary'"
            @click="query.page = n"
          >{{ n }}</button>
          <button class="btn btn-sm btn-secondary" :disabled="pageBtnDisabled(1)" @click="query.page += 1"><ChevronRight :size="14"/></button>
        </div>
      </div>
    </div>
  </div>
</template>
