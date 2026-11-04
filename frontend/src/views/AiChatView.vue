<template>
  <div class="chat-page">
    <div class="chat-window">
      <div class="messages" ref="messagesRef">
        <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
          <div class="bubble">
            <div class="role">{{ m.role === 'user' ? '我' : 'AI' }}</div>
            <div class="content">{{ m.content }}</div>
          </div>
        </div>
        <div v-if="loading" class="msg assistant">
          <div class="bubble">
            <div class="role">AI</div>
            <div class="content typing">{{ replyBuffer || '...' }}</div>
          </div>
        </div>
      </div>
      <div class="input-area">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          placeholder="向 AI 提问（如：什么是 Spring AI？）"
          @keydown.enter.exact.prevent="send"
        />
        <el-button type="primary" :loading="loading" @click="send">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { streamChat } from '@/api/chat'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<Message[]>([])
const input = ref('')
const loading = ref(false)
const replyBuffer = ref('')
const messagesRef = ref<HTMLElement | null>(null)

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  replyBuffer.value = ''
  await scrollToBottom()

  streamChat(text, {
    onMessage: (delta) => {
      replyBuffer.value += delta
      scrollToBottom()
    },
    onError: () => {
      if (replyBuffer.value) {
        messages.value.push({ role: 'assistant', content: replyBuffer.value })
      }
      replyBuffer.value = ''
      loading.value = false
    },
    onClose: () => {
      if (replyBuffer.value) {
        messages.value.push({ role: 'assistant', content: replyBuffer.value })
      }
      replyBuffer.value = ''
      loading.value = false
    }
  })
}
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 60px);
  padding: 16px;
  background: #f5f7fa;
}
.chat-window {
  max-width: 900px;
  margin: 0 auto;
  height: 100%;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.msg {
  display: flex;
}
.msg.user {
  justify-content: flex-end;
}
.bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg.user .bubble {
  background: #409eff;
  color: #fff;
}
.msg.assistant .bubble {
  background: #f5f7fa;
  color: #303133;
}
.role {
  font-size: 12px;
  opacity: 0.7;
  margin-bottom: 4px;
}
.typing::after {
  content: '▋';
  animation: blink 1s infinite;
}
@keyframes blink {
  50% { opacity: 0; }
}
.input-area {
  border-top: 1px solid #ebeef5;
  padding: 12px;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}
.input-area .el-button {
  height: 60px;
}
</style>
