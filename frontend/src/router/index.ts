import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    redirect: '/draw',
    children: [
      {
        path: 'draw',
        name: 'draw',
        component: () => import('@/views/DrawCardView.vue'),
        meta: { title: '抽卡' }
      },
      {
        path: 'bank',
        name: 'bank',
        component: () => import('@/views/QuestionBankView.vue'),
        meta: { title: '题库管理' }
      },
      {
        path: 'category',
        name: 'category',
        component: () => import('@/views/CategoryManageView.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'stats',
        name: 'stats',
        component: () => import('@/views/StatisticsView.vue'),
        meta: { title: '统计' }
      },
      {
        path: 'favorites',
        name: 'favorites',
        component: () => import('@/views/MyQuestionsView.vue'),
        meta: { title: '我的收藏', mode: 'favorites' }
      },
      {
        path: 'wrongs',
        name: 'wrongs',
        component: () => import('@/views/MyQuestionsView.vue'),
        meta: { title: '错题本', mode: 'wrongs' }
      },
      {
        path: 'history',
        name: 'history',
        component: () => import('@/views/MyQuestionsView.vue'),
        meta: { title: '历史题目', mode: 'history' }
      },
      {
        path: 'chat',
        name: 'chat',
        component: () => import('@/views/AiChatView.vue'),
        meta: { title: 'AI 聊天' }
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('@/views/SettingsView.vue'),
        meta: { title: '设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.afterEach((to) => {
  const baseTitle = '题目抽卡'
  document.title = to.meta.title ? `${to.meta.title} - ${baseTitle}` : baseTitle
})

export default router
