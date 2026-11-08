<template>
  <div class="page-container">
    <el-card>
      <div class="toolbar">
        <div class="flex-row">
          <el-select
            v-model="query.categoryId"
            placeholder="所有分类"
            clearable
            style="width: 200px"
            @change="onFilter"
          >
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
          <el-select
            v-model="query.type"
            placeholder="所有题型"
            clearable
            style="width: 160px"
            @change="onFilter"
          >
            <el-option label="单选" value="单选" />
            <el-option label="多选" value="多选" />
            <el-option label="简答" value="简答" />
          </el-select>
          <el-input
            v-model="query.keyword"
            placeholder="按题干搜索"
            clearable
            style="width: 260px"
            @keyup.enter="onFilter"
            @clear="onFilter"
          />
          <el-button type="primary" @click="onFilter">搜索</el-button>
        </div>
        <div class="flex-row">
          <el-button type="success" @click="onAdd">新增题目</el-button>
          <el-button type="warning" @click="onOpenImport">批量导入</el-button>
          <el-button
            type="danger"
            :disabled="selectedIds.length === 0"
            @click="onBatchDelete"
          >
            批量删除 ({{ selectedIds.length }})
          </el-button>
          <el-dropdown trigger="click" @command="onBackupCommand">
            <el-button>
              备份/恢复<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="export">导出整库备份</el-dropdown-item>
                <el-dropdown-item command="restore">恢复备份（覆盖现有）</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <input
            ref="restoreInputRef"
            type="file"
            accept=".json,application/json"
            style="display: none"
            @change="onRestoreFile"
          />
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        style="width: 100%"
        @selection-change="onSelectionChange"
        row-key="id"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="题干" min-width="220" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">
            <el-tag>{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="140">
          <template #default="{ row }">
            <el-tag v-if="row.categoryId == null" type="info" size="small">未分类</el-tag>
            <span v-else>{{ categoryName(row.categoryId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="answer" label="参考答案" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="success" @click="onCopy(row)">复制</el-button>
            <el-button link type="danger" @click="onDeleteOne(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager mt-16">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <QuestionForm
      v-model="formVisible"
      :categories="categories"
      :question="editTarget"
      @saved="onSaved"
    />

    <el-dialog v-model="importVisible" title="批量导入题目" width="640px">
      <div class="import-hint">
        <p>请上传 JSON 数组文件，每项包含：title / type / answer 必填；categoryName（推荐）/ categoryId / options / analysis 可选。</p>
        <p class="hint-note">推荐用 categoryName：按名称匹配已有分类，不存在会自动创建；categoryId 必须是数据库中已存在的分类 ID；都不传则归到「默认分类」。</p>
        <el-button link type="primary" @click="onDownloadTemplate">下载模板</el-button>
      </div>
      <el-upload
        drag
        accept=".json,application/json"
        :auto-upload="false"
        :limit="1"
        :on-exceed="() => ElMessage.warning('只支持上传 1 个文件')"
        :on-change="onFileChange"
        ref="uploadRef"
      >
        <el-icon><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽 JSON 文件到此处，或 <em>点击选择</em></div>
        <template #tip>
          <div v-if="parsedPreview" class="preview">
            解析到 {{ parsedPreview.length }} 道题目
            <el-button link type="primary" @click="showPreview = !showPreview">
              {{ showPreview ? '收起' : '展开预览' }}
            </el-button>
          </div>
        </template>
      </el-upload>
      <div v-if="showPreview && parsedPreview.length" class="preview-list">
        <div v-for="(q, i) in parsedPreview.slice(0, 10)" :key="i" class="preview-item">
          {{ i + 1 }}. [{{ q.type }}] {{ q.title?.slice(0, 40) }}...
        </div>
        <div v-if="parsedPreview.length > 10" class="preview-more">... 仅显示前 10 条</div>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="importing"
          :disabled="parsedPreview.length === 0"
          @click="onConfirmImport"
        >
          确认导入 ({{ parsedPreview.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, UploadFilled } from '@element-plus/icons-vue'
import type { UploadInstance, UploadFile, UploadFiles } from 'element-plus'
import QuestionForm from '@/components/QuestionForm.vue'
import { listCategoriesCached } from '@/api/category'
import { batchImportQuestions, deleteQuestions, pageQuestions } from '@/api/question'
import { exportBackup, restoreBackup } from '@/api/backup'
import type { BackupPayload } from '@/api/backup'
import { invalidateQuestionCache } from '@/composables/useLocalCache'
import type { Category, PageResult, Question, QuestionDTO, QuestionQueryDTO } from '@/types'

const categories = ref<Category[]>([])
const rows = ref<Question[]>([])
const total = ref(0)
const loading = ref(false)
const selectedIds = ref<number[]>([])
const formVisible = ref(false)
const editTarget = ref<Question | null>(null)

// 批量导入相关状态
const importVisible = ref(false)
const importing = ref(false)
const parsedPreview = ref<QuestionDTO[]>([])
const showPreview = ref(false)
const uploadRef = ref<UploadInstance>()

const query = reactive<QuestionQueryDTO>({
  page: 1,
  size: 10,
  categoryId: undefined,
  keyword: '',
  type: ''
})

const fetchCategories = async () => {
  categories.value = await listCategoriesCached()
}

const fetchList = async () => {
  loading.value = true
  try {
    const res: PageResult<Question> = await pageQuestions(query)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const onFilter = () => {
  query.page = 1
  fetchList()
}

const categoryName = (id: number | null) =>
  id == null ? '未分类' : (categories.value.find(c => c.id === id)?.categoryName || '-')

const onSelectionChange = (sel: Question[]) => {
  selectedIds.value = sel.map(s => s.id)
}

const onAdd = () => {
  editTarget.value = null
  formVisible.value = true
}
const onEdit = (row: Question) => {
  editTarget.value = { ...row }
  formVisible.value = true
}
/** 复制题目：去掉 id 后填入表单作为新增，标题加「（副本）」后缀便于区分变种题 */
const onCopy = (row: Question) => {
  const copy = { ...row, title: `${row.title}（副本）` } as Partial<Question>
  delete copy.id
  delete copy.createTime
  editTarget.value = copy as Question
  formVisible.value = true
}
const onSaved = () => {
  invalidateQuestionCache()
  fetchList()
}

const onDeleteOne = async (row: Question) => {
  await ElMessageBox.confirm(`确定删除题目「${row.title.slice(0, 20)}…」？`, '提示', {
    type: 'warning'
  })
  await deleteQuestions([row.id])
  ElMessage.success('删除成功')
  invalidateQuestionCache()
  fetchList()
}

const onBatchDelete = async () => {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selectedIds.value.length} 道题目？`, '提示', {
    type: 'warning'
  })
  const n = await deleteQuestions(selectedIds.value)
  ElMessage.success(`已删除 ${n} 道`)
  selectedIds.value = []
  invalidateQuestionCache()
  fetchList()
}

// ============ 批量导入 ============
// 模板示例：
//   - 前两题分别演示 categoryName（推荐）与 categoryId 两种分类写法；
//   - 第三题省略分类字段，导入后自动归到 id=1 的「默认分类」。
// 注意：categoryId 必须真实存在，否则整批导入会报错；categoryName 不存在会自动创建。
const TEMPLATE_JSON = [
  {
    categoryName: '默认分类',
    title: '示例题目：Java 中哪个关键字用于继承？',
    type: '单选',
    options: '[{"label":"A","text":"extends"},{"label":"B","text":"implements"},{"label":"C","text":"inherit"},{"label":"D","text":"super"}]',
    answer: 'A',
    analysis: 'extends 用于类继承，implements 用于实现接口。'
  },
  {
    // categoryId 写法：必须是数据库里已存在的分类 ID
    categoryId: 1,
    title: '示例题目：下列哪些是 Java 基本数据类型？（多选）',
    type: '多选',
    options: '[{"label":"A","text":"int"},{"label":"B","text":"String"},{"label":"C","text":"boolean"},{"label":"D","text":"Integer"}]',
    answer: 'AC',
    analysis: 'String 是引用类型，Integer 是包装类。'
  },
  {
    // 省略 categoryName / categoryId：导入后自动归到 id=1 的「默认分类」
    title: '示例题目（不指定分类）：什么是 RESTful API？',
    type: '简答',
    options: '',
    answer: 'RESTful API 是基于 HTTP 协议、采用资源 + HTTP 动词（GET/POST/PUT/DELETE）进行增删改查、无状态、可缓存的接口风格。',
    analysis: '关键点：资源定位（URI）、动作语义（HTTP method）、无状态、JSON 表征。'
  }
]

const onDownloadTemplate = () => {
  const text = JSON.stringify(TEMPLATE_JSON, null, 2)
  const blob = new Blob([text], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'questions-template.json'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('模板已下载')
}

const onOpenImport = () => {
  parsedPreview.value = []
  showPreview.value = false
  importVisible.value = true
}

const onFileChange = (file: UploadFile, _files: UploadFiles) => {
  if (!file.raw) return
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const text = e.target?.result as string
      const data = JSON.parse(text)
      if (!Array.isArray(data)) {
        ElMessage.error('JSON 必须是数组')
        parsedPreview.value = []
        return
      }
      parsedPreview.value = data as QuestionDTO[]
      showPreview.value = false
      ElMessage.success(`解析成功，共 ${parsedPreview.value.length} 道题目`)
    } catch (err) {
      ElMessage.error('JSON 解析失败：' + (err as Error).message)
      parsedPreview.value = []
    }
  }
  reader.readAsText(file.raw, 'UTF-8')
}

const onConfirmImport = async () => {
  if (parsedPreview.value.length === 0) {
    ElMessage.warning('请先上传 JSON 文件')
    return
  }
  importing.value = true
  try {
    const result = await batchImportQuestions(parsedPreview.value)
    ElMessage.success(`成功导入 ${result.length} 道题目`)
    importVisible.value = false
    parsedPreview.value = []
    showPreview.value = false
    uploadRef.value?.clearFiles()
    invalidateQuestionCache()
    fetchList()
  } finally {
    importing.value = false
  }
}

// ============ 全量备份 / 恢复 ============
const restoreInputRef = ref<HTMLInputElement>()

const onBackupCommand = async (cmd: string) => {
  if (cmd === 'export') {
    await onExportBackup()
  } else if (cmd === 'restore') {
    restoreInputRef.value?.click()
  }
}

/** 导出整库快照为 JSON 文件下载 */
const onExportBackup = async () => {
  const payload = await exportBackup()
  const text = JSON.stringify(payload, null, 2)
  const blob = new Blob([text], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `quizcard-backup-${new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success(`已导出备份：${payload.questions.length} 道题、${payload.categories.length} 个分类`)
}

/** 读取备份文件并恢复（覆盖现有题库，需二次确认） */
const onRestoreFile = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // 允许重复选择同一文件
  if (!file) return
  let payload: BackupPayload
  try {
    const text = await file.text()
    payload = JSON.parse(text) as BackupPayload
    if (!Array.isArray(payload.categories) || !Array.isArray(payload.questions)) {
      throw new Error('缺少 categories/questions 数组')
    }
  } catch (err) {
    ElMessage.error('备份文件解析失败：' + (err as Error).message)
    return
  }
  await ElMessageBox.confirm(
    `恢复将清空现有题库，并以备份中的 ${payload.questions.length} 道题、${payload.categories.length} 个分类重建。确定继续？`,
    '危险操作',
    { type: 'error', confirmButtonText: '确认恢复', cancelButtonText: '取消' }
  )
  const res = await restoreBackup(payload)
  ElMessage.success(`恢复完成：${res.questions} 道题、${res.categories} 个分类`)
  invalidateQuestionCache()
  await fetchCategories()
  fetchList()
}

onMounted(async () => {
  await fetchCategories()
  await fetchList()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.pager {
  display: flex;
  justify-content: flex-end;
}
.import-hint {
  margin-bottom: 12px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}
.import-hint p {
  margin-bottom: 6px;
}
.hint-note {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.preview {
  margin-top: 8px;
  font-size: 13px;
  color: #409eff;
}
.preview-list {
  margin-top: 8px;
  background: var(--el-fill-color-light);
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  max-height: 200px;
  overflow-y: auto;
}
.preview-item {
  padding: 4px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}
.preview-more {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-style: italic;
}
</style>
