<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { Search, ChevronLeft, ChevronRight, Filter, Download } from 'lucide-vue-next'
import type { AuditItem, Page } from '@/types'
import { getAuditList, type AuditQuery } from '@/api/audit'
import { formatNumber, formatDate } from '@/utils/format'

const loading = ref(true)
const page = reactive<Page<AuditItem>>({ list: [], total: 0, page: 1, pageSize: 20, pages: 0 })
const query = reactive<
  Required<Pick<AuditQuery, 'page' | 'pageSize'>>
  & Omit<AuditQuery, 'page' | 'pageSize' | 'keyword'>
  & { keyword: string }
>({ page: 1, pageSize: 20, type: '', result: '', keyword: '' })

async function refresh(): Promise<void> {
  loading.value = true
  try {
    const resp = await getAuditList({
      page: query.page, pageSize: query.pageSize,
      type: query.type || undefined, result: query.result || undefined,
      keyword: query.keyword || undefined
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
watch(() => [query.page, query.pageSize, query.type, query.result, query.keyword], () => refresh())

function typeLabel(t: AuditItem['type']): string {
  return ({
    login: '登录', logout: '登出', ban: '封禁', unban: '解封',
    config_change: '修改配置', case_verdict: '判决案件',
    report_generate: '生成报告', permission_change: '权限变更',
    system_restart: '系统重启'
  } as const)[t] || t
}

function resultMeta(r: AuditItem['result']) {
  return r === 'success' ? { label: '成功', cls: 'tag-green' }
    : r === 'failed'  ? { label: '失败', cls: 'tag-red' }
                       : { label: '警告', cls: 'tag-yellow' }
}

function roleLabel(r: number): string {
  return ['超级管理员','管理员','审理员','观察员'][r] || '未知'
}

function reset(): void {
  query.keyword = ''; query.type = ''; query.result = ''; query.page = 1
}

function disabled(delta: number): boolean {
  const target = page.page + delta
  return target < 1 || target > page.pages || loading.value
}
</script>

<template>
  <div class="space-y-4">
    <div class="card">
      <div class="card-body flex flex-wrap items-center gap-3">
        <div class="relative flex-1 min-w-[260px]">
          <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted"/>
          <input v-model="query.keyword" class="input pl-10" placeholder="搜索操作人 / 对象 / IP / 详情…" @keyup.enter="query.page = 1" />
        </div>
        <div class="flex items-center gap-2">
          <Filter :size="16" class="text-text-secondary"/>
          <select v-model="query.type" class="input !w-[140px]" @change="query.page = 1">
            <option value="">全部操作</option>
            <option value="login">登录</option><option value="logout">登出</option>
            <option value="ban">封禁</option><option value="unban">解封</option>
            <option value="config_change">修改配置</option><option value="case_verdict">判决案件</option>
            <option value="report_generate">生成报告</option><option value="permission_change">权限变更</option>
            <option value="system_restart">系统重启</option>
          </select>
          <select v-model="query.result" class="input !w-[120px]" @change="query.page = 1">
            <option value="">全部结果</option>
            <option value="success">成功</option>
            <option value="warning">警告</option>
            <option value="failed">失败</option>
          </select>
          <button class="btn btn-sm btn-secondary" @click="reset">重置</button>
          <button class="btn btn-sm btn-outline-cyan"><Download :size="14"/>导出</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3>审计日志 <span class="ml-2 text-caption text-text-muted">共 {{ formatNumber(page.total) }} 条</span></h3>
        <div class="text-caption text-text-secondary">保留 180 天 · 不可删除 · 不可篡改</div>
      </div>

      <div v-if="loading && page.list.length === 0" class="card-body">
        <div class="h-64 flex items-center justify-center text-text-secondary text-sm">加载中…</div>
      </div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr><th style="width: 180px;">时间</th><th>操作人</th><th>类型</th><th>目标</th><th>IP</th><th>结果</th><th>详情</th></tr>
          </thead>
          <tbody>
            <tr v-for="a in page.list" :key="a.id">
              <td class="font-mono text-caption text-text-secondary">{{ formatDate(a.time, 'full') }}</td>
              <td>
                <div class="flex items-center gap-2">
                  <div class="avatar avatar-sm">{{ a.operator.charAt(0).toUpperCase() }}</div>
                  <div>
                    <div class="text-sm text-text-primary">{{ a.operator }}</div>
                    <div class="text-caption text-text-muted">{{ roleLabel(a.operatorRole) }}</div>
                  </div>
                </div>
              </td>
              <td><span class="tag tag-blue">{{ typeLabel(a.type) }}</span></td>
              <td class="text-sm text-text-primary">{{ a.target }}</td>
              <td class="font-mono text-caption text-text-secondary">{{ a.ip }}</td>
              <td><span class="tag" :class="resultMeta(a.result).cls">{{ resultMeta(a.result).label }}</span></td>
              <td class="text-caption text-text-secondary">{{ a.detail || '-' }}</td>
            </tr>
            <tr v-if="page.list.length === 0">
              <td colspan="7" class="text-center py-16 text-text-secondary">没有符合条件的日志 <button class="btn btn-sm btn-ghost ml-2" @click="reset">清除筛选</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card-footer">
        <div class="text-caption text-text-secondary">
          第 <span class="font-mono text-text-primary">{{ page.page }}</span> / <span class="font-mono">{{ page.pages || 1 }}</span> 页 · 每页
          <select v-model.number="query.pageSize" class="inline-block mx-1 input !w-[80px] !py-1 !h-8" @change="query.page = 1">
            <option :value="20">20</option><option :value="50">50</option><option :value="100">100</option>
          </select> 条
        </div>
        <div class="flex items-center gap-1">
          <button class="btn btn-sm btn-secondary" :disabled="disabled(-1)" @click="query.page -= 1"><ChevronLeft :size="14"/></button>
          <button
            v-for="n in Array.from({ length: Math.min(5, page.pages) }, (_, i) => {
              const start = Math.max(1, Math.min(page.page - 2, page.pages - 4))
              return start + i
            }).filter((n) => n >= 1 && n <= page.pages)"
            :key="n" class="btn btn-sm" :class="n === page.page ? 'btn-primary' : 'btn-secondary'"
            @click="query.page = n"
          >{{ n }}</button>
          <button class="btn btn-sm btn-secondary" :disabled="disabled(1)" @click="query.page += 1"><ChevronRight :size="14"/></button>
        </div>
      </div>
    </div>
  </div>
</template>
