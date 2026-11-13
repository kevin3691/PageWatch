<script setup lang="ts">
import type { Monitor } from '../types'
import { computed } from 'vue'
import { LoaderCircle } from '@lucide/vue'
const props = defineProps<{ monitor: Monitor }>()
const state = computed(() =>
  props.monitor.checking
    ? 'checking'
    : !props.monitor.active
      ? 'paused'
      : props.monitor.lastError
        ? 'error'
        : props.monitor.unread
          ? 'changed'
          : 'active'
)
const labels = {
  checking: '正在检查',
  paused: '已暂停',
  error: '检查异常',
  changed: '有新变化',
  active: '监测中'
}
</script>
<template>
  <span :class="['status-badge', `status-${state}`]"
    ><LoaderCircle v-if="state === 'checking'" class="spin" :size="12" /><span
      v-else
      class="status-dot"
    />{{ labels[state] }}</span
  >
</template>
