<template>
  <div id="app">
    <Layout v-if="$route.path !== '/login'">
      <router-view />
    </Layout>
    <router-view v-else />
  </div>
</template>

<script setup>
// PSU全生命周期管理平台根组件
import Layout from '@/components/Layout.vue'
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
}
</style>