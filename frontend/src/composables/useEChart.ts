import * as echarts from 'echarts'
import { onBeforeUnmount, shallowRef, type Ref } from 'vue'

/**
 * ECharts 轻封装：按需初始化、自动随窗口 resize、组件卸载时销毁
 */
export function useEChart(elRef: Ref<HTMLElement | null>) {
  const chart = shallowRef<echarts.ECharts>()

  const resize = () => chart.value?.resize()

  const render = (option: echarts.EChartsOption) => {
    if (!elRef.value) return
    if (!chart.value) {
      chart.value = echarts.init(elRef.value)
      window.addEventListener('resize', resize)
    }
    chart.value.setOption(option, true)
  }

  onBeforeUnmount(() => {
    window.removeEventListener('resize', resize)
    chart.value?.dispose()
    chart.value = undefined
  })

  return { render, resize }
}

/** 读取 Element Plus 主题 CSS 变量，让图表跟随明暗主题 */
export function themeColor(name: string, fallback: string): string {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || fallback
}
