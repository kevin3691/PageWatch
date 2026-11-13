import { ref } from 'vue'
import type { Monitor } from '../types'
import { listMonitors } from '../api/monitors'
import { message } from './notifications'
export const monitors = ref<Monitor[]>([])
export const clockNow = ref(Date.now())
export const loading = ref(true)
export const loadError = ref('')
export const connection = ref<'connecting' | 'live' | 'offline'>('connecting')
export const revision = ref(0)
let refreshing: Promise<void> | null = null
let refreshAgain = false
export function refreshMonitors() {
  if (refreshing) {
    refreshAgain = true
    return refreshing
  }
  refreshing = listMonitors()
    .then((data) => {
      monitors.value = data
      loadError.value = ''
    })
    .catch((e) => {
      loadError.value = message(e)
    })
    .finally(() => {
      loading.value = false
      refreshing = null
      if (refreshAgain) {
        refreshAgain = false
        void refreshMonitors()
      }
    })
  return refreshing
}
export function startLive() {
  const ticker = setInterval(() => {
    clockNow.value = Date.now()
  }, 30000)
  void refreshMonitors()
  const events = new EventSource('/api/events')
  events.addEventListener('connected', () => {
    connection.value = 'live'
    revision.value++
    void refreshMonitors()
  })
  events.addEventListener('refresh', () => {
    revision.value++
    void refreshMonitors()
  })
  events.onerror = () => {
    connection.value = 'offline'
  }
  const poll = setInterval(() => {
    if (connection.value !== 'live') {
      void refreshMonitors()
      revision.value++
    }
  }, 15000)
  return () => {
    events.close()
    clearInterval(poll)
    clearInterval(ticker)
  }
}
