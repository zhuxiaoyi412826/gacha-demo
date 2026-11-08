<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="logo">🎯 题目抽卡</div>
      <el-menu
        :default-active="route.path"
        mode="horizontal"
        :ellipsis="false"
        router
        class="menu"
      >
        <el-menu-item index="/draw">抽卡</el-menu-item>
        <el-menu-item index="/bank">题库管理</el-menu-item>
        <el-menu-item index="/category">分类管理</el-menu-item>
        <el-menu-item index="/favorites">收藏</el-menu-item>
        <el-menu-item index="/wrongs">错题</el-menu-item>
        <el-menu-item index="/stats">统计</el-menu-item>
        <el-menu-item index="/chat">AI 聊天</el-menu-item>
        <el-menu-item index="/settings">设置</el-menu-item>
      </el-menu>
      <el-tooltip :content="isDark ? '切换到浅色模式' : '切换到暗黑模式'" placement="bottom">
        <el-button circle class="theme-btn" @click="toggleDark">
          <el-icon><Sunny v-if="isDark" /><Moon v-else /></el-icon>
        </el-button>
      </el-tooltip>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useDarkMode } from '@/composables/useDarkMode'

const route = useRoute()
const { isDark, toggleDark } = useDarkMode()
</script>

<style scoped>
.layout {
  height: 100vh;
}
.header {
  background: var(--el-bg-color);
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-light);
  padding: 0 24px;
  height: 60px;
}
.theme-btn {
  margin-left: 16px;
  flex-shrink: 0;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  margin-right: 32px;
  white-space: nowrap;
}
.menu {
  border-bottom: none !important;
  flex: 1;
}
.main {
  padding: 0;
  overflow-y: auto;
}
</style>
