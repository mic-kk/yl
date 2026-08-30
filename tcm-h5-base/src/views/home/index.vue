<template>
  <div class="home-page">
    <van-nav-bar title="首页" />
    <van-cell-group inset>
      <van-cell title="当前用户" :value="userStore.userInfo?.realName || '-'" />
      <van-cell title="登录账号" :value="userStore.userInfo?.username || '-'" />
    </van-cell-group>
    <div class="home-tip">H5 基座脚手架完成：登录鉴权、请求封装、移动端适配已打通。</div>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.home-tip {
  margin: 16px;
  color: #969799;
  font-size: 13px;
}
</style>
