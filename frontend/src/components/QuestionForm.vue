<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑题目' : '新增题目'"
    width="640px"
    @close="onClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="题目分类" prop="categoryId">
        <el-select
          v-model="form.categoryId"
          placeholder="不选即归到「默认分类」"
          clearable
          style="width: 100%"
        >
          <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="题干" prop="title">
        <el-input v-model="form.title" type="textarea" :rows="3" placeholder="请输入题干" />
      </el-form-item>
      <el-form-item label="题目类型" prop="type">
        <el-radio-group v-model="form.type">
          <el-radio-button label="单选" />
          <el-radio-button label="多选" />
          <el-radio-button label="简答" />
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.type !== '简答'" label="选项" prop="options">
        <el-input
          v-model="form.options"
          type="textarea"
          :rows="4"
          placeholder='JSON 数组，如：[{"label":"A","text":"选项一"},{"label":"B","text":"选项二"}]'
        />
        <div class="hint">一行一个 JSON 对象，包含 label 和 text 字段</div>
      </el-form-item>
      <el-form-item label="参考答案" prop="answer">
        <el-input v-model="form.answer" type="textarea" :rows="2" placeholder="请输入参考答案" />
      </el-form-item>
      <el-form-item label="解析">
        <el-input v-model="form.analysis" type="textarea" :rows="2" placeholder="解析（可选）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { addQuestion, updateQuestion } from '@/api/question'
import type { Category, Question, QuestionDTO, QuestionType } from '@/types'

const props = defineProps<{
  modelValue: boolean
  categories: Category[]
  question?: Question | null
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved'): void
}>()

const visible = ref(props.modelValue)
watch(() => props.modelValue, (v) => {
  visible.value = v
  if (v) {
    // 先清 id，避免「复制题目」或上次编辑残留的 id 导致误判为更新
    form.id = undefined
    if (props.question) {
      Object.assign(form, props.question)
    } else {
      Object.assign(form, {
        id: undefined,
        categoryId: null,
        title: '',
        type: '单选' as QuestionType,
        options: '',
        answer: '',
        analysis: ''
      })
    }
  }
})
watch(visible, (v) => emit('update:modelValue', v))

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive<QuestionDTO>({
  categoryId: null,
  title: '',
  type: '单选',
  options: '',
  answer: '',
  analysis: ''
})
const rules: FormRules = {
  title: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  answer: [{ required: true, message: '请输入参考答案', trigger: 'blur' }]
}

const onSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.type !== '简答') {
      // 校验 options 是合法 JSON
      try {
        if (form.options) JSON.parse(form.options)
      } catch {
        ElMessage.error('选项必须是合法 JSON')
        return
      }
    } else {
      form.options = ''
    }
    submitting.value = true
    try {
      if (form.id) {
        await updateQuestion(form)
      } else {
        await addQuestion(form)
      }
      ElMessage.success(form.id ? '更新成功' : '新增成功')
      emit('saved')
      visible.value = false
    } finally {
      submitting.value = false
    }
  })
}

const onClose = () => {
  formRef.value?.resetFields()
}
</script>

<style scoped>
.hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
