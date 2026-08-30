<template>
  <el-container class="layout">
    <el-aside width="220px" class="layout-aside">
      <div class="layout-logo">TCM 平台</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#409eff"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <el-dropdown @command="handleCommand">
          <span class="layout-user">
            {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// TODO: 后续由后端返回菜单/按钮权限，前端动态渲染，禁止硬编码权限
const menus = computed(() => [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
])

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.layout-aside {
  background-color: #001529;
}
.layout-logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}
.layout-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-bottom: 1px solid #eee;
}
.layout-user {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
