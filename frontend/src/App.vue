<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  Radar,
  LayoutGrid,
  Bell,
  ArrowUpRight,
  Leaf,
  CircleCheck,
  CircleAlert,
  X
} from '@lucide/vue'
import { monitors, connection, startLive } from './stores/monitors'
import { toasts } from './stores/notifications'
const route = useRoute()
const unread = computed(() => monitors.value.filter((m) => m.unread).length)
let stop: (() => void) | undefined
onMounted(() => {
  stop = startLive()
})
onUnmounted(() => stop?.())
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink to="/" class="brand" aria-label="页哨首页"
        ><span class="brand-mark"><Radar :size="26" :stroke-width="1.8" /></span
        ><span>页哨<small>PAGEWATCH</small></span></RouterLink
      >
      <div class="workspace-label">我的工作空间 <span>PERSONAL</span></div>
      <nav aria-label="主导航">
        <RouterLink
          to="/"
          :class="[
            'nav-link',
            { selected: route.path === '/' && route.query.filter !== 'changed' }
          ]"
          ><LayoutGrid :size="18" />关注列表<span class="nav-count">{{
            monitors.length
          }}</span></RouterLink
        >
        <RouterLink
          to="/?filter=changed"
          :class="['nav-link', { selected: route.query.filter === 'changed' }]"
          ><Bell :size="18" />未读变化<span v-if="unread" class="nav-count unread-count">{{
            unread
          }}</span></RouterLink
        >
      </nav>
      <div class="sidebar-note">
        <div class="note-icon"><Leaf :size="19" /></div>
        <strong>少一点刷新，多一点专注。</strong>
        <p>值得关注的更新，<br />页哨会替你留意。</p>
        <RouterLink to="/new">添加一个关注 <ArrowUpRight :size="14" /></RouterLink>
      </div>
      <div class="sidebar-bottom">
        <span :class="['live-dot', { offline: connection !== 'live' }]" />{{
          connection === 'live'
            ? '实时连接已建立'
            : connection === 'offline'
              ? '连接中断，正在重连'
              : '正在连接服务'
        }}<span class="version">v0.1</span>
      </div>
    </aside>
    <div class="main-shell">
      <header class="topbar">
        <span class="breadcrumb"
          >工作空间 <span>/</span>
          <strong>{{
            route.path === '/new'
              ? '添加关注'
              : route.path.startsWith('/monitors/')
                ? '变化详情'
                : '我的关注'
          }}</strong></span
        ><span class="topbar-tag"><span class="tiny-dot" /> PERSONAL RADAR</span>
      </header>
      <main id="main-content"><RouterView /></main>
      <footer class="app-footer">
        <span>PAGEWATCH <span class="footer-dot">·</span> 让每一次更新都有迹可循</span
        ><span>为专注而造 <Leaf :size="12" /></span>
      </footer>
    </div>
    <div class="toast-stack" aria-live="polite">
      <div v-for="item in toasts" :key="item.id" :class="['toast', { 'toast-error': item.error }]">
        <CircleAlert v-if="item.error" :size="18" /><CircleCheck v-else :size="18" /><span>{{
          item.text
        }}</span
        ><button
          class="icon-button"
          aria-label="关闭提示"
          @click="toasts = toasts.filter((t) => t.id !== item.id)"
        >
          <X :size="14" />
        </button>
      </div>
    </div>
  </div>
</template>
