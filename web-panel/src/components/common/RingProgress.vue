<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'

use([
  CanvasRenderer,
  PieChart,
  TooltipComponent
])

interface Props {
  value: number
  color?: string
  size?: number
  trackColor?: string
  label?: string | number
  sublabel?: string
}

const props = withDefaults(defineProps<Props>(), {
  color: '#00E5FF',
  size: 120,
  trackColor: '#1C2333'
})

const pct = computed(() => Math.max(0, Math.min(100, Math.round(props.value))))

const option = computed(() => ({
  tooltip: {
    show: false
  },
  series: [
    {
      type: 'pie',
      radius: ['72%', '86%'],
      center: ['50%', '50%'],
      silent: true,
      avoidLabelOverlap: false,
      label: { show: false },
      labelLine: { show: false },
      startAngle: 90,
      data: [
        {
          value: pct.value,
          itemStyle: {
            color: props.color,
            borderRadius: 999
          }
        },
        {
          value: 100 - pct.value,
          itemStyle: {
            color: props.trackColor
          }
        }
      ]
    }
  ]
}))
</script>

<template>
  <div
    class="relative inline-flex items-center justify-center"
    :style="{ width: size + 'px', height: size + 'px' }"
  >
    <v-chart
      class="w-full h-full"
      :option="option"
    />
    <!-- 中央文字 -->
    <div class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
      <div class="text-[26px] font-bold text-text-primary leading-none">
        {{ label !== undefined ? label : pct + '%' }}
      </div>
      <div v-if="sublabel" class="text-xs text-text-secondary mt-1">
        {{ sublabel }}
      </div>
    </div>
  </div>
</template>
