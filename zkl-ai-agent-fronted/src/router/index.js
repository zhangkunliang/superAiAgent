import { createRouter, createWebHistory } from 'vue-router'
import AuthLanding from '../views/AuthLanding.vue'
import Dashboard from '../views/Dashboard.vue'
import InspectorApp from '../views/InspectorApp.vue'
import ManusApp from '../views/ManusApp.vue'
import TestMessage from '../components/TestMessage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'auth-landing',
      component: AuthLanding,
      meta: { requiresGuest: true }
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: Dashboard,
      meta: { requiresAuth: true }
    },
    {
      path: '/inspector-app/:chatId?',
      name: 'inspector-app',
      component: InspectorApp,
      meta: { requiresAuth: true }
    },
    {
      path: '/manus-app/:chatId?',
      name: 'manus-app',
      component: ManusApp,
      meta: { requiresAuth: true }
    },
    {
      path: '/test',
      name: 'test',
      component: TestMessage,
      meta: { requiresAuth: true }
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const { useAuthStore } = await import('../store/auth')
  const authStore = useAuthStore()
  
  // 初始化认证状态
  if (authStore.token && !authStore.user) {
    try {
      await authStore.initAuth()
    } catch (error) {
      console.error('初始化认证状态失败:', error)
    }
  }
  
  // 检查是否需要登录
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({
      name: 'auth-landing',
      query: { redirect: to.fullPath }
    })
    return
  }
  
  // 检查是否需要游客状态（已登录用户不能访问登录/注册页面）
  if (to.meta.requiresGuest && authStore.isAuthenticated) {
    next({ name: 'dashboard' })
    return
  }
  
  next()
})

export default router 