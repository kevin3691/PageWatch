<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Plus,
  Search,
  RefreshCw,
  ArrowUpRight,
  Globe2,
  Clock3,
  Layers,
  Radio,
  Bell,
  CircleAlert,
  Pause,
  Play,
  Radar,
  ScanText,
  GitCompareArrows,
  ChevronRight,
  LoaderCircle
} from '@lucide/vue'
import StatusBadge from '../components/StatusBadge.vue'
import { checkMonitor, checkAllMonitors, updateMonitor, DEMO_URL } from '../api/monitors'
import { monitors, loading, loadError, refreshMonitors } from '../stores/monitors'
import { toast, message } from '../stores/notifications'
import { hostname, relative, intervalLabel } from '../utils/format'
import type { Monitor } from '../types'
const route = useRoute(),
  router = useRouter()
const query = ref(''),
  checkingAll = ref(false),
  toggling = ref('')
const filter = computed(() => String(route.query.filter ?? 'all'))
const tabs = [
  { id: 'all', label: '全部关注' },
  { id: 'active', label: '监测中' },
  { id: 'changed', label: '有新变化' },
  { id: 'paused', label: '已暂停' },
  { id: 'error', label: '检查异常' }
]
const active = computed(() => monitors.value.filter((m) => m.active).length)
const changed = computed(() => monitors.value.filter((m) => m.unread).length)
const errors = computed(() => monitors.value.filter((m) => m.lastError && m.active).length)
const filtered = computed(() =>
  monitors.value.filter((m) => {
    const matches =
      filter.value === 'all' ||
      (filter.value === 'active' && m.active) ||
      (filter.value === 'changed' && m.unread) ||
      (filter.value === 'paused' && !m.active) ||
      (filter.value === 'error' && !!m.lastError && m.active)
    return matches && `${m.name} ${m.url}`.toLowerCase().includes(query.value.toLowerCase())
  })
)
async function check(m: Monitor) {
  try {
    const result = await checkMonitor(m.id)
    if (result.accepted) {
      m.checking = true
      toast('已开始检查，完成后会自动刷新')
    }
  } catch (e) {
    toast(message(e), true)
  }
}
async function checkAll() {
  checkingAll.value = true
  try {
    const result = await checkAllMonitors()
    toast(result.accepted ? `已开始检查 ${result.accepted} 个网页` : '当前没有需要检查的网页')
    await refreshMonitors()
  } catch (e) {
    toast(message(e), true)
  } finally {
    checkingAll.value = false
  }
}
async function toggle(m: Monitor) {
  toggling.value = m.id
  try {
    await updateMonitor(m.id, { name: m.name, intervalMinutes: m.intervalMinutes, active: !m.active })
    await refreshMonitors()
    toast(m.active ? '已暂停自动检查' : '已恢复自动检查')
  } catch (e) {
    toast(message(e), true)
  } finally {
    toggling.value = ''
  }
}
function clearFilter() {
  query.value = ''
  void router.replace('/')
}
</script>

<template>
  <div class="page dashboard">
    <div class="page-heading">
      <div>
        <div class="eyebrow"><span class="tiny-dot" /> YOUR WEB, IN FOCUS</div>
        <h1>替你留意，每一次变化<span class="accent-period">。</span></h1>
        <p class="page-description">把重要的网页放在这里，更新发生时，一眼就能看到。</p>
      </div>
      <RouterLink to="/new" class="button primary"><Plus :size="17" />添加关注</RouterLink>
    </div>
    <section class="stats-grid" aria-label="关注统计">
      <div class="stat-card">
        <div>
          <span class="stat-label">全部关注</span
          ><strong>{{ monitors.length }}<small>个网页</small></strong>
        </div>
        <span class="stat-icon"><Globe2 :size="20" /></span>
      </div>
      <div class="stat-card">
        <div>
          <span class="stat-label">正在监测</span
          ><strong>{{ active }}<small>持续守候</small></strong>
        </div>
        <span class="stat-icon green"><Radio :size="20" /></span>
      </div>
      <div class="stat-card">
        <div>
          <span class="stat-label">未读变化</span
          ><strong>{{ changed }}<small>等待查看</small></strong>
        </div>
        <span class="stat-icon amber"><Bell :size="20" /></span>
      </div>
      <div class="stat-card">
        <div>
          <span class="stat-label">检查异常</span
          ><strong>{{ errors }}<small>需要留意</small></strong>
        </div>
        <span class="stat-icon"><CircleAlert :size="20" /></span>
      </div>
    </section>
    <div class="section-heading">
      <h2>
        我的关注 <span>{{ monitors.length }}</span>
      </h2>
      <button class="button small subtle" :disabled="checkingAll || active === 0" @click="checkAll">
        <RefreshCw :size="14" :class="{ spin: checkingAll }" />全部检查
      </button>
    </div>
    <div class="list-toolbar">
      <div class="filter-tabs" aria-label="筛选关注">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          :class="{ selected: filter === tab.id }"
          @click="router.replace({ path: '/', query: tab.id === 'all' ? {} : { filter: tab.id } })"
        >
          {{ tab.label }}<span v-if="tab.id === 'changed' && changed">{{ changed }}</span>
        </button>
      </div>
      <label class="search-box"
        ><Search :size="16" /><input
          v-model="query"
          aria-label="搜索关注"
          placeholder="搜索名称或网址…"
      /></label>
    </div>
    <div v-if="loadError" class="error-panel">
      <CircleAlert :size="24" />
      <h3>暂时连接不上服务</h3>
      <p>{{ loadError }}</p>
      <button class="button" @click="refreshMonitors">重新连接</button>
    </div>
    <div v-else-if="loading" class="loading-panel">
      <LoaderCircle class="spin" :size="24" />正在加载你的关注…
    </div>
    <template v-else-if="monitors.length === 0">
      <section class="empty-hero">
        <div class="empty-illustration" aria-hidden="true">
          <div class="radar-circle circle-outer" />
          <div class="radar-circle circle-middle" />
          <div class="radar-circle circle-inner" />
          <div class="radar-axis horizontal" />
          <div class="radar-axis vertical" />
          <div class="radar-sweep" />
          <span class="radar-point point-one" /><span class="radar-point point-two" /><span
            class="radar-center"
            ><Radar :size="30" :stroke-width="1.5" /></span
          ><span class="radar-float"><span class="live-dot" />发现值得关注的变化</span>
        </div>
        <div class="empty-copy">
          <span class="pill-label">你的个人网页雷达</span>
          <h2>重要的更新，<br />不用再反复刷新。</h2>
          <p>更新日志、活动公告、心仪的职位……<br />添加一个网址，让变化主动来到你面前。</p>
          <div class="button-row">
            <RouterLink to="/new" class="button primary"
              ><Plus :size="16" />添加第一个关注</RouterLink
            ><RouterLink to="/new?demo=1" class="button text-button"
              >先体验演示 <ArrowUpRight :size="15"
            /></RouterLink>
          </div>
          <small class="empty-tip">支持公开静态网页 · 自动保存版本 · 只关注正文</small>
        </div>
      </section>
      <div class="feature-grid">
        <div>
          <span class="feature-number">01</span><ScanText :size="21" />
          <h3>只看你在意的部分</h3>
          <p>自动提取正文，或用选择器指定区域，过滤导航与页脚。</p>
        </div>
        <div>
          <span class="feature-number">02</span><GitCompareArrows :size="21" />
          <h3>每处变化，一目了然</h3>
          <p>新增与删除逐行高亮，不必在长篇内容中寻找不同。</p>
        </div>
        <div>
          <span class="feature-number">03</span><Clock3 :size="21" />
          <h3>让更新有迹可循</h3>
          <p>按设定的频率自动检查，随时翻看过去的每一个版本。</p>
        </div>
      </div>
    </template>
    <div v-else-if="filtered.length === 0" class="empty-filter">
      <Search :size="28" />
      <h3>没有找到符合条件的关注</h3>
      <p>试试其他关键词或筛选条件。</p>
      <button class="button small" @click="clearFilter">查看全部关注</button>
    </div>
    <template v-else
      ><div class="monitor-grid">
        <article
          v-for="m in filtered"
          :key="m.id"
          :class="['monitor-card', { 'has-change': m.unread }]"
        >
          <div class="card-top">
            <span class="site-icon">{{
              hostname(m.url).replace('www.', '').charAt(0).toUpperCase()
            }}</span
            ><StatusBadge :monitor="m" />
          </div>
          <RouterLink :to="`/monitors/${m.id}`" class="card-title"
            >{{ m.name }}<ArrowUpRight :size="17" /></RouterLink
          ><span v-if="m.url === DEMO_URL" class="site-url">内置演示网页 · 无需联网</span>
          <a v-else :href="m.url" target="_blank" rel="noopener noreferrer" class="site-url">{{
            hostname(m.url)
          }}</a>
          <p v-if="m.lastError" class="card-error">{{ m.lastError }}</p>
          <div v-else class="card-meta">
            <span><Clock3 :size="13" />每 {{ intervalLabel(m.intervalMinutes) }}</span
            ><span><Layers :size="13" />{{ m.snapshotCount }} 个版本</span>
          </div>
          <div class="card-bottom">
            <RouterLink :to="`/monitors/${m.id}`" class="last-checked"
              >{{ m.unread ? '查看新变化' : '检查于 ' + relative(m.lastCheckedAt)
              }}<ChevronRight v-if="m.unread" :size="13"
            /></RouterLink>
            <div class="card-actions">
              <button
                class="icon-button"
                :aria-label="m.active ? '暂停关注' : '恢复关注'"
                :title="m.active ? '暂停' : '恢复'"
                :disabled="toggling === m.id"
                @click="toggle(m)"
              >
                <Pause v-if="m.active" :size="15" /><Play v-else :size="15" /></button
              ><button
                class="icon-button"
                :disabled="m.checking"
                aria-label="立即检查"
                title="立即检查"
                @click="check(m)"
              >
                <RefreshCw :size="15" :class="{ spin: m.checking }" />
              </button>
            </div>
          </div>
        </article>
        <RouterLink to="/new" class="add-card"
          ><span><Plus :size="22" /></span><strong>添加新的关注</strong>
          <p>下一个值得留意的网页</p></RouterLink
        >
      </div>
      <div class="list-footnote">
        <span class="tiny-dot" />网页正在按各自的频率检查，变化会自动出现在这里。
      </div></template
    >
  </div>
</template>
