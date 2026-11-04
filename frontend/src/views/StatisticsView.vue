<template>
  <div class="page-container">
    <div class="cards">
      <el-card class="stat-card">
        <div class="stat-value">{{ drawCount }}</div>
        <div class="stat-label">抽卡总次数</div>
      </el-card>
      <el-card class="stat-card clickable" @click="goPage('/favorites')">
        <div class="stat-value warning">{{ favoriteCount }}</div>
        <div class="stat-label">收藏题目数（点击查看）</div>
      </el-card>
      <el-card class="stat-card clickable" @click="goPage('/wrongs')">
        <div class="stat-value danger">{{ wrongCount }}</div>
        <div class="stat-label">错题数（点击查看）</div>
      </el-card>
      <el-card class="stat-card clickable" @click="goPage('/history')">
        <div class="stat-value success">{{ historyCount }}</div>
        <div class="stat-label">历史题目（点击查看）</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value primary">{{ totalQuestions }}</div>
        <div class="stat-label">题目总数</div>
      </el-card>
    </div>

    <el-card class="mt-24">
      <h3>各分类题目数量</h3>
      <el-table :data="rows" style="width: 100%" class="mt-16">
        <el-table-column prop="categoryName" label="分类名称" />
        <el-table-column label="题目数">
          <template #default="{ row }">
            <el-progress
              :percentage="percent(row.count)"
              :format="() => `${row.count}`"
            />
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="rows.length === 0" description="暂无分类数据" />
    </el-card>

    <!-- 抽卡趋势：按天 / 周 / 月 -->
    <el-card class="mt-24">
      <div class="chart-head">
        <h3>抽卡趋势</h3>
        <el-radio-group v-model="trendRange" size="small">
          <el-radio-button value="day">按天</el-radio-button>
          <el-radio-button value="week">按周</el-radio-button>
          <el-radio-button value="month">按月</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="trendRef" class="chart"></div>
      <div class="chart-tip">数据来自本地抽卡记录（最近 20000 次）</div>
    </el-card>

    <!-- 错题率 Top 10：柱状图 + 表格 -->
    <el-card class="mt-24">
      <h3>错题率 Top 10</h3>
      <el-empty v-if="wrongRateRows.length === 0" description="暂无错题数据" />
      <template v-else>
        <div ref="wrongRateRef" class="chart wrong-chart"></div>
        <el-table :data="wrongRateRows" style="width: 100%" class="mt-16">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="categoryName" label="分类" min-width="140" />
          <el-table-column prop="wrong" label="错题数" width="100" />
          <el-table-column prop="total" label="题目总数" width="110" />
          <el-table-column label="错题率" min-width="180">
            <template #default="{ row }">
              <el-progress :percentage="row.rate" :format="() => `${row.rate}%`" />
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import { listCategoriesCached } from '@/api/category'
import { countByCategoryCached, findByIds } from '@/api/question'
import { useDrawStats } from '@/composables/useDrawStats'
import { useDarkMode } from '@/composables/useDarkMode'
import { themeColor, useEChart } from '@/composables/useEChart'
import type { Category, CategoryCount } from '@/types'

const router = useRouter()
const { isDark } = useDarkMode()
const { drawCount, drawLog, wrongs, favoriteCount, wrongCount, historyCount } = useDrawStats()

/** 收藏/错题/历史卡片统一跳转到对应列表页 */
const goPage = (path: string) => router.push(path)

interface Row {
  id: number
  categoryName: string
  count: number
}
const rows = ref<Row[]>([])

const totalQuestions = computed(() => rows.value.reduce((s, r) => s + r.count, 0))

const percent = (count: number) => {
  const total = totalQuestions.value || 1
  return Math.round((count / total) * 100)
}

// ============ 抽卡趋势图 ============
type TrendRange = 'day' | 'week' | 'month'
const trendRange = ref<TrendRange>('day')
const trendRef = ref<HTMLElement | null>(null)
const trendChart = useEChart(trendRef)

const pad = (n: number) => String(n).padStart(2, '0')

/** 把抽卡时间戳按天/周/月分桶，返回横轴标签与次数 */
function bucketDrawLog(log: number[], range: TrendRange) {
  const now = new Date()
  const buckets: { label: string; start: number; end: number }[] = []

  if (range === 'day') {
    // 最近 14 天（含今天）
    for (let i = 13; i >= 0; i--) {
      const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() - i)
      const end = new Date(now.getFullYear(), now.getMonth(), now.getDate() - i + 1)
      buckets.push({
        label: `${pad(start.getMonth() + 1)}-${pad(start.getDate())}`,
        start: start.getTime(),
        end: end.getTime()
      })
    }
  } else if (range === 'week') {
    // 最近 12 周（以周一为一周起点）
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const offset = (today.getDay() + 6) % 7
    const thisMonday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - offset)
    for (let i = 11; i >= 0; i--) {
      const start = new Date(thisMonday.getFullYear(), thisMonday.getMonth(), thisMonday.getDate() - i * 7)
      const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 7)
      const lastDay = new Date(end.getFullYear(), end.getMonth(), end.getDate() - 1)
      buckets.push({
        label: `${pad(start.getMonth() + 1)}-${pad(start.getDate())}~${pad(lastDay.getMonth() + 1)}-${pad(lastDay.getDate())}`,
        start: start.getTime(),
        end: end.getTime()
      })
    }
  } else {
    // 最近 12 个月
    for (let i = 11; i >= 0; i--) {
      const start = new Date(now.getFullYear(), now.getMonth() - i, 1)
      const end = new Date(now.getFullYear(), now.getMonth() - i + 1, 1)
      buckets.push({
        label: `${start.getFullYear()}-${pad(start.getMonth() + 1)}`,
        start: start.getTime(),
        end: end.getTime()
      })
    }
  }

  const values = buckets.map(
    (b) => log.filter((t) => t >= b.start && t < b.end).length
  )
  return { labels: buckets.map((b) => b.label), values }
}

const renderTrend = () => {
  const { labels, values } = bucketDrawLog(drawLog.value, trendRange.value)
  const textColor = themeColor('--el-text-color-regular', '#606266')
  const primary = themeColor('--el-color-primary', '#409eff')
  const splitLine = themeColor('--el-border-color-lighter', '#ebeef5')

  const option: EChartsOption = {
    grid: { left: 44, right: 24, top: 24, bottom: 40 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'line' } },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { color: textColor, fontSize: 12 },
      axisLine: { lineStyle: { color: splitLine } }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: textColor },
      splitLine: { lineStyle: { color: splitLine } }
    },
    series: [
      {
        name: '抽卡次数',
        type: 'line',
        data: values,
        smooth: true,
        symbolSize: 6,
        lineStyle: { color: primary, width: 2 },
        itemStyle: { color: primary },
        areaStyle: { color: 'rgba(64, 158, 255, 0.15)' }
      }
    ]
  }
  trendChart.render(option)
}

watch(trendRange, renderTrend)

// ============ 错题率 Top 10 ============
interface WrongRateRow {
  id: number
  categoryName: string
  wrong: number
  total: number
  rate: number
}
const wrongRateRows = ref<WrongRateRow[]>([])
const wrongRateRef = ref<HTMLElement | null>(null)
const wrongRateChart = useEChart(wrongRateRef)

const loadWrongRate = async (categories: Category[], counts: CategoryCount[]) => {
  if (wrongs.value.length === 0) {
    wrongRateRows.value = []
    return
  }
  // 错题 ID → 题目详情，拿到所属分类
  const wrongQuestions = await findByIds(wrongs.value)
  const wrongByCategory = new Map<number, number>()
  wrongQuestions.forEach((q) => {
    const cid = q.categoryId ?? 1
    wrongByCategory.set(cid, (wrongByCategory.get(cid) ?? 0) + 1)
  })

  wrongRateRows.value = categories
    .map((cat) => {
      const total = counts.find((c) => c.categoryId === cat.id)?.count ?? 0
      const wrong = wrongByCategory.get(cat.id) ?? 0
      const rate = total > 0 ? Number(((wrong / total) * 100).toFixed(1)) : 0
      return { id: cat.id, categoryName: cat.categoryName, wrong, total, rate }
    })
    .filter((r) => r.wrong > 0)
    .sort((a, b) => b.rate - a.rate || b.wrong - a.wrong)
    .slice(0, 10)
}

const renderWrongRateChart = async () => {
  if (wrongRateRows.value.length === 0) return
  await nextTick()
  if (!wrongRateRef.value) return

  // 横向柱状图：数值越高的分类排在最上方
  const sorted = [...wrongRateRows.value].reverse()
  const textColor = themeColor('--el-text-color-regular', '#606266')
  const splitLine = themeColor('--el-border-color-lighter', '#ebeef5')
  const danger = themeColor('--el-color-danger', '#f56c6c')

  const option: EChartsOption = {
    grid: { left: 120, right: 60, top: 12, bottom: 24 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const p = (params as { name: string; value: number }[])[0]
        return `${p.name}<br/>错题率：${p.value}%`
      }
    },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { color: textColor, formatter: '{value}%' },
      splitLine: { lineStyle: { color: splitLine } }
    },
    yAxis: {
      type: 'category',
      data: sorted.map((r) => r.categoryName),
      axisLabel: { color: textColor, fontSize: 12 },
      axisLine: { lineStyle: { color: splitLine } }
    },
    series: [
      {
        name: '错题率',
        type: 'bar',
        data: sorted.map((r) => r.rate),
        barWidth: 14,
        itemStyle: { color: danger, borderRadius: [0, 4, 4, 0] },
        label: {
          show: true,
          position: 'right',
          color: textColor,
          formatter: ({ value }) => `${value}%`
        }
      }
    ]
  }
  wrongRateChart.render(option)
}

// 主题切换后重新渲染，保证坐标轴/文字颜色跟随明暗模式
watch(isDark, () => {
  renderTrend()
  renderWrongRateChart()
})

const fetchAll = async () => {
  const [list, c] = await Promise.all([listCategoriesCached(), countByCategoryCached()])
  rows.value = list.map((cat) => ({
    id: cat.id,
    categoryName: cat.categoryName,
    count: c.find((x) => x.categoryId === cat.id)?.count ?? 0
  }))
  await loadWrongRate(list, c)
  renderTrend()
  await renderWrongRateChart()
}

onMounted(fetchAll)
</script>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}
.stat-card {
  text-align: center;
}
.stat-card.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}
.stat-card.clickable:hover {
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.25);
  transform: translateY(-2px);
}
.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}
.stat-value.primary { color: #409eff; }
.stat-value.warning { color: #e6a23c; }
.stat-value.danger { color: #f56c6c; }
.stat-value.success { color: #67c23a; }
.stat-label {
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.chart-head h3 {
  margin: 0;
}
.chart {
  width: 100%;
  height: 300px;
  margin-top: 12px;
}
.wrong-chart {
  height: 320px;
}
.chart-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-top: 4px;
}
</style>
