<script setup lang="ts">
import { ref, computed, useSlots, type VNode, type Component } from 'vue'

export interface DataTableColumn {
  key: string
  title: string
  width?: string
  align?: 'left' | 'right' | 'center'
  render?: (row: any) => VNode
}

export interface RowAction {
  label: string
  icon?: Component
  onClick: (row: any) => void
  danger?: boolean
}

interface Props {
  columns: DataTableColumn[]
  data: any[]
  loading?: boolean
  selectable?: boolean
  rowActions?: RowAction[]
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  selectable: false,
  rowActions: () => []
})

const emit = defineEmits<{
  selectionChanged: [rows: any[]]
}>()

const slots = useSlots()
const selectedKeys = ref<Set<string>>(new Set())

const allSelected = computed(() => {
  if (!props.data.length) return false
  return props.data.every((_, i) => selectedKeys.value.has(String(i)))
})

const someSelected = computed(() => {
  return selectedKeys.value.size > 0 && !allSelected.value
})

function getRowKey(row: any, index: number): string {
  return row.uuid || row.id || String(index)
}

function isSelected(row: any, index: number): boolean {
  const k = getRowKey(row, index)
  return selectedKeys.value.has(k)
}

function toggleRow(row: any, index: number): void {
  const k = getRowKey(row, index)
  if (selectedKeys.value.has(k)) {
    selectedKeys.value.delete(k)
  } else {
    selectedKeys.value.add(k)
  }
  emitSelection()
}

function toggleAll(): void {
  if (allSelected.value) {
    selectedKeys.value.clear()
  } else {
    props.data.forEach((row, i) => {
      selectedKeys.value.add(getRowKey(row, i))
    })
  }
  emitSelection()
}

function emitSelection(): void {
  const rows = props.data.filter((row, i) => selectedKeys.value.has(getRowKey(row, i)))
  emit('selectionChanged', rows)
}

function alignClass(align?: string): string {
  switch (align) {
    case 'right': return 'text-right'
    case 'center': return 'text-center'
    default: return 'text-left'
  }
}
</script>

<template>
  <div class="table-wrap relative">
    <!-- Loading overlay -->
    <div
      v-if="loading"
      class="absolute inset-0 z-10 flex items-center justify-center bg-bg-card/60 backdrop-blur-[1px] rounded-card"
    >
      <span class="text-sm text-text-secondary">加载中…</span>
    </div>

    <table class="data-table w-full">
      <thead class="sticky top-0 z-[1]">
        <tr>
          <th v-if="selectable" class="w-10 text-center" style="width: 40px">
            <input
              type="checkbox"
              :checked="allSelected"
              :indeterminate="someSelected"
              @change="toggleAll"
              class="cursor-pointer"
            />
          </th>
          <th
            v-for="col in columns"
            :key="col.key"
            :class="alignClass(col.align)"
            :style="col.width ? { width: col.width } : undefined"
          >
            {{ col.title }}
          </th>
          <th v-if="rowActions && rowActions.length" class="w-auto text-right" style="width: 120px">
            操作
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="data.length === 0 && !loading">
          <td
            :colspan="(selectable ? 1 : 0) + columns.length + (rowActions && rowActions.length ? 1 : 0)"
            class="text-center text-text-secondary py-10"
          >
            暂无数据
          </td>
        </tr>
        <tr v-for="(row, rowIdx) in data" :key="rowIdx" class="group">
          <td v-if="selectable" class="text-center">
            <input
              type="checkbox"
              :checked="isSelected(row, rowIdx)"
              @change="toggleRow(row, rowIdx)"
              class="cursor-pointer"
            />
          </td>
          <td
            v-for="col in columns"
            :key="col.key"
            :class="alignClass(col.align)"
          >
            <!-- Slot: cell-xxx 优先 -->
            <slot
              v-if="slots['cell-' + col.key]"
              :name="'cell-' + col.key"
              :row="row"
              :index="rowIdx"
            />
            <!-- render 函数 -->
            <component
              v-else-if="col.render"
              :is="col.render(row)"
            />
            <!-- 默认值 -->
            <span v-else>{{ row[col.key] }}</span>
          </td>
          <td v-if="rowActions && rowActions.length" class="text-right">
            <div class="inline-flex items-center gap-1 transition-opacity duration-150" :class="loading ? 'opacity-100' : 'md:opacity-0 md:group-hover:opacity-100'">
              <button
                v-for="(action, aIdx) in rowActions"
                :key="aIdx"
                type="button"
                class="btn btn-sm"
                :class="action.danger ? 'btn-danger' : 'btn-secondary'"
                @click="action.onClick(row)"
              >
                <component v-if="action.icon" :is="action.icon" :size="14" />
                <span>{{ action.label }}</span>
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
