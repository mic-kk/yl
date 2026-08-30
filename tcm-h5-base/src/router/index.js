import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { title: '首页' },
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({ history: createWebHistory(), routes })

// 路由守卫：未登录跳登录页；已登录访问登录页跳首页
router.beforeEach((to) => {
  const userStore = useUserStore()
  const isLoggedIn = !!userStore.token
  if (to.path !== '/login' && !isLoggedIn) {
    return '/login'
  }
  if (to.path === '/login' && isLoggedIn) {
    return '/'
  }
})

export default router
