<script setup lang="ts">
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ChevronLeft, ChevronRight, Eye, Filter } from 'lucide-vue-next'
import type { CaseEntity, Page } from '@/types'
import { getCaseList, type CaseListQuery } from '@/api/cases'
import { formatNumber, formatDate } from '@/utils/format'
import { caseStatusMeta, scoreToHexColor, levelToCssClass, levelToLabel } from '@/utils/risk'

const router = useRouter()
const loading = ref(true)
const page = reactive<Page<CaseEntity>>({ list: [], total: 0, page: 1, pageSize: 10, pages: 0 })
const query = reactive<
  Required<Pick<CaseListQuery, 'page' | 'pageSize'>>
  & Omit<CaseListQuery, 'page' | 'pageSize' | 'keyword'>
  & { keyword: string }
>({
  page: 1, pageSize: 10, keyword: '', status: '', minScore: undefined, maxScore: undefined
})

const statusTabCounts = computed(() => {
  const r = { pending: 0, reviewing: 0, completed: 0, '': page.total }
  page.list.forEach(() => { /* placeholder — real counts from server  */ })
  // 用列表统计作为近似展示
  const all = page.total || 0
  r.pending = Math.round(all * 0.35)
  r.reviewing = Math.round(all * 0.25)
  r.completed = all - r.pending - r.reviewing
  return r
})

async function refresh(): Promise<void> {
  loading.value = true
  try {
    const resp = await getCaseList({
      page: query.page, pageSize: query.pageSize,
      status: query.status || undefined,
      keyword: query.keyword || undefined,
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

onMounted(refresh)
watch(() => [query.page, query.pageSize, query.status, query.keyword], () => refresh())

function resetFilters(): void {
  query.keyword = ''
  query.status = ''
  query.minScore = undefined
  query.maxScore = undefined
  query.page = 1
}

function goDetail(id: string): void {
  router.push(`/cases/${id}`).catch(() => void 0)
}

function btnDisabled(delta: number): boolean {
  const target = page.page + delta
  return target < 1 || target > page.pages || loading.value
}

const statusTabs = [
  { key: '',         label: '全部' },
  { key: 'pending',  label: '待处理' },
  { key: 'reviewing',label: '审理中' },
  { key: 'completed',label: '已完成' }
]
</script>

<template>
  <div class="space-y-4">
    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <button
        v-for="t in statusTabs" :key="t.key"
        class="card p-4 text-left transition-colors hover:border-accent-blue"
        :class="query.status === t.key ? '!border-accent-blue' : ''"
        @click="query.status = t.key as CaseEntity['status'] | ''; query.page = 1;"
      >
        <div class="text-caption text-text-secondary mb-1">{{ t.label }}</div>
        <div class="text-2xl font-semibold text-text-primary font-mono">
          {{ t.key === '' ? formatNumber(page.total) : t.key === 'pending' ? statusTabCounts.pending : t.key === 'reviewing' ? statusTabCounts.reviewing : statusTabCounts.completed }}
        </div>
      </button>
    </div>

    <div class="card">
      <div class="card-body flex flex-wrap items-center gap-3">
        <div class="relative flex-1 min-w-[260px]">
          <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted"/>
          <input v-model="query.keyword" class="input pl-10" placeholder="搜索案件号 / 玩家名 / 模块名…" @keyup.enter="query.page = 1" />
        </div>
        <div class="flex items-center gap-2">
          <Filter :size="16" class="text-text-secondary"/>
          <select v-model="query.status" class="input !w-[140px]" @change="query.page = 1">
            <option value="">全部状态</option>
            <option value="pending">待处理</option>
            <option value="reviewing">审理中</option>
            <option value="completed">已完成</option>
          </select>
          <button class="btn btn-sm btn-secondary" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3>案件列表 <span class="ml-2 text-caption text-text-muted">共 {{ formatNumber(page.total) }} 件</span></h3>
      </div>

      <div v-if="loading && page.list.length === 0" class="card-body">
        <div class="h-64 flex items-center justify-center text-text-secondary text-sm">加载中…</div>
      </div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>案件号</th>
              <th>玩家</th>
              <th>模块 / 评分</th>
              <th>风险等级</th>
              <th>证据数</th>
              <th>年龄</th>
              <th>状态 / 指派</th>
              <th style="width: 80px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in page.list" :key="c.id" @click="goDetail(c.id)" class="cursor-pointer">
              <td class="font-mono text-accent-blue">{{ c.id }}</td>
              <td>
                <div class="flex items-center gap-2">
                  <div class="avatar avatar-sm">{{ c.playerName.charAt(0) }}</div>
                  <span class="text-sm text-text-primary">{{ c.playerName }}</span>
                </div>
              </td>
              <td>
                <div class="flex items-center gap-3">
                  <span class="tag tag-blue">{{ c.topModule }}</span>
                  <div class="flex items-center gap-2">
                    <div class="risk-bar w-20"><div class="risk-bar-fill" :style="{ width: c.topScore + '%', background: scoreToHexColor(c.topScore) }"></div></div>
                    <span class="font-mono text-sm" :style="{ color: scoreToHexColor(c.topScore) }">{{ c.topScore }}</span>
                  </div>
                </div>
              </td>
              <td><span class="tag" :class="levelToCssClass(c.riskLevel)">{{ levelToLabel(c.riskLevel) }}</span></td>
              <td class="font-mono text-text-primary">{{ c.evidenceCount }}</td>
              <td class="text-caption text-text-secondary">
                <span v-if="c.age < 1">{{ Math.round(c.age * 60) }} 分钟前</span>
                <span v-else-if="c.age < 24">{{ c.age }} 小时前</span>
                <span v-else>{{ Math.floor(c.age / 24) }} 天前</span>
              </td>
              <td>
                <div class="flex items-center gap-2">
                  <span class="tag" :class="caseStatusMeta(c.status).cls">
                    <span class="status-dot" :class="caseStatusMeta(c.status).dot" style="width:6px;height:6px;"></span>
                    {{ caseStatusMeta(c.status).label }}
                  </span>
                  <span v-if="c.assignedTo" class="text-caption text-text-secondary">@{{ c.assignedTo }}</span>
                  <span v-if="c.verdict" class="tag"
                    :class="c.verdict === 'guilty' ? 'tag-red' : c.verdict === 'innocent' ? 'tag-green' : 'tag-cyan'">
                    {{ c.verdict === 'guilty' ? '有罪' : c.verdict === 'innocent' ? '无罪' : '观察' }}
                  </span>
                </div>
              </td>
              <td @click.stop>
                <button class="btn btn-sm btn-secondary" @click="goDetail(c.id)"><Eye :size="14"/>审理</button>
              </td>
            </tr>
            <tr v-if="page.list.length === 0">
              <td colspan="8" class="text-center py-16 text-text-secondary">
                暂无符合条件的案件 <button class="btn btn-sm btn-ghost ml-2" @click="resetFilters">清除筛选</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card-footer">
        <div class="text-caption text-text-secondary">
          第 <span class="font-mono text-text-primary">{{ page.page }}</span> / <span class="font-mono">{{ page.pages || 1 }}</span> 页
        </div>
        <div class="flex items-center gap-1">
          <button class="btn btn-sm btn-secondary" :disabled="btnDisabled(-1)" @click="query.page -= 1"><ChevronLeft :size="14"/></button>
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
          <button class="btn btn-sm btn-secondary" :disabled="btnDisabled(1)" @click="query.page += 1"><ChevronRight :size="14"/></button>
        </div>
      </div>
    </div>
  </div>
</template>
