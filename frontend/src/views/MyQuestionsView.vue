<template>
  <div class="page-container">
    <el-card>
      <div class="head-row">
        <h3>{{ title }}（{{ questions.length }}）</h3>
        <el-button text type="primary" @click="$router.push('/stats')">返回统计</el-button>
      </div>
      <el-empty v-if="!loading && questions.length === 0" :description="emptyText" />
      <div v-else v-loading="loading" class="list">
        <div
          v-for="(q, i) in questions"
          :key="q.id"
          class="row"
          @click="openDetail(i)"
        >
          <span class="idx">{{ i + 1 }}</span>
          <span class="t" :title="q.title">{{ q.title }}</span>
          <el-tag size="small">{{ q.type }}</el-tag>
          <span class="cat">{{ categoryName(q.categoryId) }}</span>
        </div>
      </div>
    </el-card>

    <!-- 题目详情弹窗：上一题/下一题顺序浏览 -->
    <el-dialog
      v-model="visible"
      width="760px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <template #header>
        <div class="dlg-head">
          <el-tag size="small">{{ current?.type }}</el-tag>
          <span class="cat">{{ categoryName(current?.categoryId ?? null) }}</span>
          <span class="progress">{{ index + 1 }} / {{ questions.length }}</span>
        </div>
      </template>
      <div v-if="current" class="dlg-body">
        <h3 class="dlg-title">{{ current.title }}</h3>
        <div v-if="current.options && current.type !== '简答'" class="opts">
          <div v-for="(o, i) in parseOptions(current.options)" :key="i" class="opt">
            <b>{{ o.label }}.</b> {{ o.text }}
          </div>
        </div>
        <div class="sec">
          <div class="sec-label">参考答案</div>
          <div v-if="settings.markdownEnabled" class="markdown-body" :style="answerStyle" v-html="renderMarkdown(current.answer)"></div>
          <div v-else class="markdown-body plain-text" :style="answerStyle">{{ current.answer }}</div>
        </div>
        <div v-if="current.analysis" class="sec">
          <div class="sec-label">解析</div>
          <div
            v-if="settings.markdownEnabled"
            class="markdown-body analysis"
            :style="answerStyle"
            v-html="renderMarkdown(current.analysis)"
          ></div>
          <div v-else class="markdown-body analysis plain-text" :style="answerStyle">{{ current.analysis }}</div>
        </div>
      </div>
      <template #footer>
        <el-button :disabled="index <= 0" @click="go(-1)">← 上一题</el-button>
        <el-button
          type="primary"
          :disabled="index >= questions.length - 1"
          @click="go(1)"
        >
          下一题 →
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { listCategoriesCached } from '@/api/category'
import { findByIds } from '@/api/question'
import { useDrawStats } from '@/composables/useDrawStats'
import { FONT_SIZE_MAP, LINE_HEIGHT_MAP, useSettings } from '@/composables/useSettings'
import { renderMarkdown } from '@/utils/markdown'
import { playPage } from '@/utils/sound'
import type { Category, Question } from '@/types'

type Mode = 'favorites' | 'wrongs' | 'history'

const route = useRoute()
const mode = computed(() => (route.meta.mode as Mode) || 'favorites')

const title = computed(() =>
  mode.value === 'favorites' ? '我的收藏' : mode.value === 'wrongs' ? '错题本' : '历史题目'
)
const emptyText = computed(() =>
  mode.value === 'favorites'
    ? '暂无收藏题目，去抽卡页收藏几道吧'
    : mode.value === 'wrongs'
      ? '暂无错题，继续保持'
      : '还没有抽过题，去抽卡页开始吧'
)

const { favorites, wrongs, drawHistory } = useDrawStats()
const { settings } = useSettings()
// 答案排版设置：与抽卡页保持一致
const answerStyle = computed(() => ({
  fontSize: FONT_SIZE_MAP[settings.answerFontSize],
  lineHeight: String(LINE_HEIGHT_MAP[settings.answerLineHeight])
}))
const categories = ref<Category[]>([])
const questions = ref<Question[]>([])
const loading = ref(false)

const visible = ref(false)
const index = ref(0)
const current = computed(() => questions.value[index.value] || null)

const categoryName = (id: number | null) =>
  id == null ? '默认分类' : categories.value.find((c) => c.id === id)?.categoryName || '-'

const parseOptions = (opts?: string | null) => {
  if (!opts) return []
  try {
    const arr = JSON.parse(opts)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

const openDetail = (i: number) => {
  index.value = i
  visible.value = true
}

/** 顺序浏览：到头/尾时按钮禁用，展示完全部为止 */
const go = (delta: number) => {
  const next = index.value + delta
  if (next < 0 || next >= questions.value.length) return
  index.value = next
  playPage()
}

onMounted(async () => {
  loading.value = true
  try {
    categories.value = await listCategoriesCached()
    const ids =
      mode.value === 'favorites'
        ? favorites.value
        : mode.value === 'wrongs'
          ? wrongs.value
          : drawHistory.value
    questions.value = ids.length ? await findByIds(ids) : []
    // findByIds 按创建时间排序；这里按本地记录顺序（最近记录在前）
    const order = new Map(ids.map((id, i) => [id, i]))
    questions.value = [...questions.value].sort(
      (a, b) => (order.get(b.id) ?? 0) - (order.get(a.id) ?? 0)
    )
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.head-row h3 {
  margin: 0;
}
.list {
  display: flex;
  flex-direction: column;
}
.row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.row:hover {
  background: var(--el-fill-color-light);
}
.row + .row {
  border-top: 1px dashed var(--el-border-color-lighter);
}
.idx {
  width: 28px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  flex-shrink: 0;
}
.t {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cat {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  flex-shrink: 0;
}
.dlg-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.progress {
  margin-left: auto;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.dlg-title {
  margin: 0 0 12px;
  line-height: 1.6;
}
.opts {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}
.opt {
  background: var(--el-fill-color-light);
  border-radius: 6px;
  padding: 6px 10px;
}
.opt b {
  color: var(--el-color-primary);
  margin-right: 4px;
}
.sec {
  margin-top: 12px;
}
.sec-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--el-color-primary);
  margin-bottom: 6px;
}
.analysis {
  color: var(--el-text-color-regular);
}
.plain-text {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
