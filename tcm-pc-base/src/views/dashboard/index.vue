<template>
  <el-card>
    <h3>欢迎使用 TCM 平台</h3>
    <p>当前用户：{{ userStore.userInfo?.realName }}（{{ userStore.userInfo?.username }}）</p>
    <p>脚手架已完成：Sa-Token 登录鉴权、统一返回、全局异常、审计日志链路已打通。</p>
  </el-card>
</template>

<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>
