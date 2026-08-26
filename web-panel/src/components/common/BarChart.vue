<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart as EChartsBarChart } from 'echarts/charts'
import {
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  EChartsBarChart,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent
])

export interface BarSeries {
  name: string
  data: number[]
  color?: string
  type?: 'line' | 'bar'
}

interface Props {
  xLabels: string[]
  series: BarSeries[]
  height?: string
  orient?: 'horizontal' | 'vertical'
}

const props = withDefaults(defineProps<Props>(), {
  height: '320px',
  orient: 'horizontal'
})

const option = computed(() => {
  const colors = props.series.map((s) => s.color || '#388BFD')
  const isHorizontal = props.orient === 'horizontal'

  return {
    color: colors,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'var(--bg-card)',
      borderColor: 'var(--border-line)',
      borderWidth: 1,
      textStyle: { color: 'var(--text-primary)', fontSize: 12 }
    },
    legend: {
      show: props.series.length > 1,
      top: 0,
      right: 0,
      textStyle: { color: 'var(--text-secondary)', fontSize: 12 },
      itemWidth: 14,
      itemHeight: 8,
      icon: 'roundRect'
    },
    grid: {
      left: isHorizontal ? 80 : 8,
      right: 16,
      top: props.series.length > 1 ? 36 : 16,
      bottom: 8,
      containLabel: true
    },
    xAxis: isHorizontal
      ? {
          type: 'value',
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(48,54,61,0.5)', type: 'dashed' } },
          axisLabel: { color: 'var(--text-secondary)', fontSize: 11 }
        }
      : {
          type: 'category',
          data: props.xLabels,
          axisLine: { lineStyle: { color: 'var(--border-line)' } },
          axisTick: { show: false },
          axisLabel: { color: 'var(--text-secondary)', fontSize: 11 }
        },
    yAxis: isHorizontal
      ? {
          type: 'category',
          data: props.xLabels,
          axisLine: { lineStyle: { color: 'var(--border-line)' } },
          axisTick: { show: false },
          axisLabel: { color: 'var(--text-secondary)', fontSize: 11 }
        }
      : {
          type: 'value',
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(48,54,61,0.5)', type: 'dashed' } },
          axisLabel: { color: 'var(--text-secondary)', fontSize: 11 }
        },
    series: props.series.map((s) => ({
      name: s.name,
      type: 'bar',
      data: s.data,
      barWidth: isHorizontal ? '55%' : '45%',
      itemStyle: {
        borderRadius: isHorizontal ? [0, 4, 4, 0] : [4, 4, 0, 0]
      }
    }))
  }
})
</script>

<template>
  <v-chart
    class="w-full"
    :style="{ height }"
    :option="option"
    autoresize
  />
</template>
