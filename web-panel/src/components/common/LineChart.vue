<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart as EChartsLineChart, BarChart as EChartsBarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  EChartsLineChart,
  EChartsBarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

export interface ChartSeries {
  name: string
  data: number[]
  color?: string
  type?: 'line' | 'bar'
}

interface Props {
  xLabels: string[]
  series: ChartSeries[]
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  height: '320px'
})

const option = computed(() => {
  const colors = props.series.map((s) => s.color || '#388BFD')
  return {
    color: colors,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
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
      left: 8,
      right: 16,
      top: props.series.length > 1 ? 36 : 16,
      bottom: 8,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: props.xLabels,
      boundaryGap: false,
      axisLine: { lineStyle: { color: 'var(--border-line)' } },
      axisTick: { show: false },
      axisLabel: { color: 'var(--text-secondary)', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: 'rgba(48,54,61,0.5)', type: 'dashed' } },
      axisLabel: { color: 'var(--text-secondary)', fontSize: 11 }
    },
    series: props.series.map((s) => ({
      name: s.name,
      type: s.type || 'line',
      data: s.data,
      smooth: true,
      showSymbol: false,
      lineStyle: s.type === 'bar' ? undefined : { width: 2 },
      areaStyle: s.type === 'bar' || !s.type || s.type === 'line' ? (s.type === 'bar' ? undefined : {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: (s.color || '#388BFD') + '33' },
            { offset: 1, color: (s.color || '#388BFD') + '05' }
          ]
        }
      }) : undefined,
      barWidth: s.type === 'bar' ? '50%' : undefined,
      itemStyle: s.type === 'bar' ? { borderRadius: [4, 4, 0, 0] } : undefined
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
