<template>
  <div class="page-container">
    <el-card>
      <div class="toolbar">
        <h2>题目分类</h2>
        <el-button type="primary" @click="onAdd">新增分类</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="categoryName" label="分类名称" />
        <el-table-column prop="createTime" label="创建时间" width="200">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="题目数" width="100">
          <template #default="{ row }">
            <el-tag>{{ countOf(row.id) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="editId ? '编辑分类' : '新增分类'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.categoryName" maxlength="100" show-word-limit placeholder="请输入分类名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addCategory,
  deleteCategory,
  listCategoriesCached,
  updateCategory
} from '@/api/category'
import { countByCategoryCached } from '@/api/question'
import { invalidateQuestionCache } from '@/composables/useLocalCache'
import type { Category, CategoryCount } from '@/types'

const rows = ref<Category[]>([])
const loading = ref(false)
const counts = ref<CategoryCount[]>([])
const formVisible = ref(false)
const editId = ref<number | null>(null)
const submitting = ref(false)
const form = reactive({ categoryName: '' })

const fetchAll = async () => {
  loading.value = true
  try {
    const [list, c] = await Promise.all([listCategoriesCached(), countByCategoryCached()])
    rows.value = list
    counts.value = c
  } finally {
    loading.value = false
  }
}

const countOf = (cid: number) => counts.value.find(x => x.categoryId === cid)?.count ?? 0

const formatTime = (t?: string) => {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 19)
}

const onAdd = () => {
  editId.value = null
  form.categoryName = ''
  formVisible.value = true
}
const onEdit = (row: Category) => {
  editId.value = row.id
  form.categoryName = row.categoryName
  formVisible.value = true
}

const onSubmit = async () => {
  if (!form.categoryName.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  submitting.value = true
  try {
    if (editId.value) {
      await updateCategory(editId.value, form.categoryName.trim())
      ElMessage.success('更新成功')
    } else {
      await addCategory(form.categoryName.trim())
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    invalidateQuestionCache() // 分类变更，清除缓存
    await fetchAll()
  } finally {
    submitting.value = false
  }
}

const onDelete = async (row: Category) => {
  await ElMessageBox.confirm(`确定删除分类「${row.categoryName}」？`, '提示', {
    type: 'warning'
  })
  try {
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    invalidateQuestionCache()
    await fetchAll()
  } catch {
    // 拦截器已提示
  }
}

onMounted(fetchAll)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
