<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  ExternalLink,
  RefreshCw,
  Pause,
  Play,
  Settings2,
  Trash2,
  Clock3,
  Layers,
  Check,
  GitCompareArrows,
  FileText,
  History,
  LoaderCircle,
  CircleAlert,
  Sparkles,
  Download,
  Columns2,
  List,
  X,
  Plus as PlusIcon
} from '@lucide/vue'
import StatusBadge from '../components/StatusBadge.vue'
import AppDialog from '../components/AppDialog.vue'
import { getMonitor, getSnapshot, checkMonitor, markRead, updateMonitor, deleteMonitor, advanceDemo, DEMO_URL } from '../api/monitors'
import { revision, refreshMonitors } from '../stores/monitors'
import { toast, message } from '../stores/notifications'
import { dateTime, intervalLabel } from '../utils/format'
import type { Detail, SnapshotDetail, Snapshot } from '../types'
const route = useRoute(),
  router = useRouter()
const data = ref<Detail | null>(null),
  snapshot = ref<SnapshotDetail | null>(null)
const error = ref(''),
  snapshotError = ref(''),
  busy = ref(''),
  selected = ref(''),
  tab = ref('diff'),
  split = ref(true)
const loadingSnapshot = ref(false),
  editOpen = ref(false),
  deleteOpen = ref(false),
  editName = ref(''),
  editInterval = ref(360)
const monitor = computed(() => data.value?.monitor)
const isDemo = computed(() => monitor.value?.url === DEMO_URL)
let detailSequence = 0,
  snapshotSequence = 0
async function load() {
  const seq = ++detailSequence
  try {
    const result = await getMonitor(route.params.id)
    if (seq !== detailSequence) return
    const followingLatest = !selected.value || selected.value === data.value?.snapshots[0]?.id
    data.value = result
    error.value = ''
    if (followingLatest || !result.snapshots.some((s) => s.id === selected.value))
      selected.value = result.snapshots[0]?.id ?? ''
  } catch (e) {
    if (seq === detailSequence) error.value = message(e)
  }
}
watch(
  () => route.params.id,
  () => {
    data.value = null
    selected.value = ''
    snapshot.value = null
    void load()
  },
  { immediate: true }
)
watch(revision, () => {
  void load()
})
watch(selected, async (id) => {
  const seq = ++snapshotSequence
  if (!id) {
    snapshot.value = null
    return
  }
  loadingSnapshot.value = true
  snapshotError.value = ''
  snapshot.value = null
  try {
    const result = await getSnapshot(route.params.id, id)
    if (seq === snapshotSequence) snapshot.value = result
  } catch (e) {
    if (seq === snapshotSequence) snapshotError.value = message(e)
  } finally {
    if (seq === snapshotSequence) loadingSnapshot.value = false
  }
})
async function action(type: string) {
  const m = monitor.value
  if (!m) return
  busy.value = type
  try {
    if (type === 'check' || type === 'demo') {
      if (type === 'demo') await advanceDemo()
      const result = await checkMonitor(m.id)
      if (result.accepted) m.checking = true
      toast(
        type === 'demo'
          ? '演示网页已更新，正在检查差异'
          : result.accepted
            ? '已开始检查'
            : '这个网页正在检查中'
      )
    } else if (type === 'read') {
      await markRead(m.id)
      await load()
      toast('已标记为已读')
    } else if (type === 'toggle') {
      await updateMonitor(m.id, { name: m.name, intervalMinutes: m.intervalMinutes, active: !m.active })
      toast(m.active ? '已暂停自动检查' : '已恢复自动检查')
      await load()
    } else if (type === 'delete') {
      await deleteMonitor(m.id)
      await refreshMonitors()
      toast('关注及其历史记录已删除')
      await router.push('/')
    } else if (type === 'edit') {
      await updateMonitor(m.id, { name: editName.value.trim(), intervalMinutes: editInterval.value, active: m.active })
      editOpen.value = false
      await load()
      toast('设置已保存')
    }
    await refreshMonitors()
  } catch (e) {
    toast(message(e), true)
  } finally {
    busy.value = ''
  }
}
function openEdit() {
  if (monitor.value) {
    editName.value = monitor.value.name
    editInterval.value = monitor.value.intervalMinutes
    editOpen.value = true
  }
}
function snapshotTitle(s: Snapshot, i: number) {
  return !s.previousId ? '首次快照' : i === 0 ? '最新版本' : '历史版本'
}
function selectSnapshot(id: string) {
  selected.value = id
  if (tab.value === 'checks') tab.value = 'diff'
}
function download() {
  if (!snapshot.value) return
  const url = URL.createObjectURL(
    new Blob([snapshot.value.content], { type: 'text/plain;charset=utf-8' })
  )
  const a = document.createElement('a')
  a.href = url
  a.download = `pagewatch-${selected.value}.txt`
  a.click()
  URL.revokeObjectURL(url)
}
const outcomeLabels = {
  BASELINE: '首次快照',
  CHANGED: '发现变化',
  UNCHANGED: '没有变化',
  FAILED: '检查失败'
}
</script>

<template>
  <div class="page detail-page">
    <RouterLink to="/" class="back-link"><ArrowLeft :size="15" />返回关注列表</RouterLink>
    <div v-if="error" class="error-panel">
      <CircleAlert :size="25" />
      <h3>{{ error }}</h3>
      <button class="button" @click="load">重新加载</button>
    </div>
    <div v-else-if="!data || !monitor" class="loading-panel">
      <LoaderCircle class="spin" />正在加载关注详情…
    </div>
    <template v-else>
      <div class="detail-heading">
        <div class="detail-title">
          <div class="eyebrow">EVERY CHANGE TELLS A STORY</div>
          <div class="title-with-badge">
            <h1>{{ monitor.name }}</h1>
            <StatusBadge :monitor="monitor" />
          </div>
          <span v-if="isDemo" class="detail-url"
            >{{ monitor.url }} <span class="mini-tag">演示网页</span></span
          ><a
            v-else
            :href="monitor.url"
            target="_blank"
            rel="noopener noreferrer"
            class="detail-url"
            >{{ monitor.url }}<ExternalLink :size="13"
          /></a>
        </div>
        <div class="detail-heading-actions">
          <button
            class="button primary"
            :disabled="monitor.checking || !!busy"
            @click="action('check')"
          >
            <RefreshCw :size="15" :class="{ spin: monitor.checking }" />{{
              monitor.checking ? '正在检查…' : '立即检查'
            }}</button
          ><button
            class="button icon-only"
            aria-label="编辑设置"
            title="编辑设置"
            @click="openEdit"
          >
            <Settings2 :size="17" />
          </button>
        </div>
      </div>
      <div class="detail-meta-bar">
        <span><Clock3 :size="15" />每 {{ intervalLabel(monitor.intervalMinutes) }}检查一次</span
        ><span><Layers :size="15" />{{ monitor.snapshotCount }} 个版本</span
        ><span>上次检查 {{ dateTime(monitor.lastCheckedAt) }}</span
        ><span v-if="monitor.selector" class="selector-tag">{{ monitor.selector }}</span>
        <div class="meta-actions">
          <button
            v-if="monitor.unread"
            class="text-action green-text"
            :disabled="!!busy"
            @click="action('read')"
          >
            <Check :size="14" />标为已读</button
          ><button class="text-action" :disabled="!!busy" @click="action('toggle')">
            <Pause v-if="monitor.active" :size="14" /><Play v-else :size="14" />{{
              monitor.active ? '暂停' : '恢复'
            }}</button
          ><button class="text-action danger-text" @click="deleteOpen = true">
            <Trash2 :size="14" />删除
          </button>
        </div>
      </div>
      <div v-if="monitor.lastError" class="info-banner error-banner">
        <CircleAlert :size="18" />
        <div>
          <strong>最近一次检查未能完成</strong>
          <p>{{ monitor.lastError }}。已保留上一次成功的快照，将在下次检查时重试。</p>
        </div>
      </div>
      <div v-if="isDemo" class="info-banner demo-banner">
        <Sparkles :size="18" /><span
          >这是内置演示网页。发布一次更新，再选择最新版本，就能看到新增和删除的内容。</span
        ><button
          class="button small"
          :disabled="monitor.checking || !!busy"
          @click="action('demo')"
        >
          <PlusIcon />发布演示更新
        </button>
      </div>
      <div class="detail-grid">
        <aside class="panel history-panel">
          <div class="history-heading">
            <History :size="17" />
            <h2>版本时间线</h2>
            <span>{{ data.snapshots.length }}</span>
          </div>
          <div class="history-list">
            <button
              v-for="(s, i) in data.snapshots"
              :key="s.id"
              :class="['history-item', { selected: selected === s.id }]"
              @click="selectSnapshot(s.id)"
            >
              <span class="timeline-dot" /><span class="history-item-content"
                ><strong
                  >{{ snapshotTitle(s, i)
                  }}<span v-if="i === 0" class="latest-label">LATEST</span></strong
                ><time>{{ dateTime(s.createdAt) }}</time
                ><span v-if="s.previousId" class="change-counts"
                  ><span>+{{ s.addedCount }} 行</span><span>−{{ s.removedCount }} 行</span></span
                ><span v-else class="baseline-label">变化对比的起点</span></span
              >
            </button>
            <div v-if="data.snapshots.length === 0" class="history-empty">首次快照还未生成</div>
          </div>
          <div class="history-tip">仅在正文发生变化时，<br />保存一个新版本。</div>
        </aside>
        <section class="panel diff-panel">
          <div class="detail-tabs">
            <div role="tablist" aria-label="内容视图">
              <button
                role="tab"
                :aria-selected="tab === 'diff'"
                :class="{ selected: tab === 'diff' }"
                @click="tab = 'diff'"
              >
                <GitCompareArrows :size="16" />变化对比</button
              ><button
                role="tab"
                :aria-selected="tab === 'content'"
                :class="{ selected: tab === 'content' }"
                @click="tab = 'content'"
              >
                <FileText :size="16" />完整正文</button
              ><button
                role="tab"
                :aria-selected="tab === 'checks'"
                :class="{ selected: tab === 'checks' }"
                @click="tab = 'checks'"
              >
                <History :size="16" />检查记录
              </button>
            </div>
            <button
              v-if="tab !== 'checks' && snapshot"
              class="icon-button"
              aria-label="导出当前正文"
              title="导出正文"
              @click="download"
            >
              <Download :size="16" />
            </button>
          </div>
          <div v-if="tab === 'checks'" class="checks-list">
            <div class="checks-intro">
              最近 {{ data.checks.length }} 次检查<span
                >自动检查在后端运行，关闭页面也会继续。</span
              >
            </div>
            <div v-for="run in data.checks" :key="run.id" class="check-row">
              <span :class="['check-outcome', run.outcome.toLowerCase()]"
                ><span class="status-dot" />{{ outcomeLabels[run.outcome] }}</span
              ><time>{{ dateTime(run.checkedAt) }}</time
              ><span class="check-duration">{{ run.durationMs }} ms</span>
              <p v-if="run.error">{{ run.error }}</p>
            </div>
            <div v-if="!data.checks.length" class="empty-filter">暂无检查记录</div>
          </div>
          <div v-else-if="loadingSnapshot" class="loading-panel">
            <LoaderCircle class="spin" />正在读取版本…
          </div>
          <div v-else-if="snapshotError" class="error-panel">{{ snapshotError }}</div>
          <template v-else-if="snapshot"
            ><template v-if="tab === 'diff' && snapshot.snapshot.previousId"
              ><div class="diff-summary">
                <span
                  ><span class="green-text">+{{ snapshot.snapshot.addedCount }} 行新增</span
                  ><span class="red-text">−{{ snapshot.snapshot.removedCount }} 行删除</span></span
                >
                <div class="view-toggle">
                  <button
                    :class="{ selected: split }"
                    title="左右对比"
                    aria-label="左右对比"
                    @click="split = true"
                  >
                    <Columns2 :size="14" /></button
                  ><button
                    :class="{ selected: !split }"
                    title="统一视图"
                    aria-label="统一视图"
                    @click="split = false"
                  >
                    <List :size="14" />
                  </button>
                </div>
              </div>
              <div v-if="split" class="split-header">
                <span>上一版本</span
                ><span
                  >当前版本 <time>{{ dateTime(snapshot.snapshot.createdAt) }}</time></span
                >
              </div>
              <div class="diff-body" :class="{ 'unified-diff': !split }">
                <div
                  v-for="(line, index) in snapshot.diff"
                  :key="index"
                  :class="['diff-row', line.kind]"
                  :data-kind="line.kind"
                >
                  <template v-if="split"
                    ><div :class="['diff-cell', { blank: line.kind === 'add' }]">
                      <span class="line-number">{{ line.oldLine }}</span
                      ><span class="line-sign">{{ line.kind === 'remove' ? '−' : '' }}</span
                      ><span>{{ line.kind === 'add' ? '' : line.text }}</span>
                    </div>
                    <div :class="['diff-cell', { blank: line.kind === 'remove' }]">
                      <span class="line-number">{{ line.newLine }}</span
                      ><span class="line-sign">{{ line.kind === 'add' ? '+' : '' }}</span
                      ><span>{{ line.kind === 'remove' ? '' : line.text }}</span>
                    </div></template
                  >
                  <div v-else class="diff-cell">
                    <span class="line-number">{{ line.oldLine }}</span
                    ><span class="line-number">{{ line.newLine }}</span
                    ><span class="line-sign">{{
                      line.kind === 'add' ? '+' : line.kind === 'remove' ? '−' : ''
                    }}</span
                    ><span>{{ line.text }}</span>
                  </div>
                </div>
              </div></template
            ><template v-else
              ><div v-if="tab === 'diff'" class="baseline-notice">
                <span class="baseline-icon"><Check :size="18" /></span>
                <div>
                  <strong>首次快照已保存</strong>
                  <p>下一次正文变化时，这里会逐行显示新增与删除。</p>
                </div>
              </div>
              <div v-else class="content-summary">
                {{ snapshot.content.length.toLocaleString() }} 字符<span>{{
                  dateTime(snapshot.snapshot.createdAt)
                }}</span>
              </div>
              <div class="full-content">
                <div v-for="(line, index) in snapshot.content.split('\n')" :key="index">
                  <span class="line-number">{{ index + 1 }}</span
                  ><span>{{ line }}</span>
                </div>
              </div></template
            ></template
          >
          <div v-else class="empty-filter">暂无快照，请先检查这个网页。</div>
        </section>
      </div></template
    >
    <AppDialog v-model="editOpen" labelledby="edit-title">
      <div class="modal-heading">
        <h2 id="edit-title">关注设置</h2>
        <button class="icon-button" aria-label="关闭" @click="editOpen = false">
          <X :size="18" />
        </button>
      </div>
      <form @submit.prevent="action('edit')">
        <label for="edit-name">关注名称</label
        ><input id="edit-name" v-model="editName" required maxlength="120" autofocus /><label
          for="edit-frequency"
          class="label-spaced"
          >检查间隔（分钟）</label
        ><input
          id="edit-frequency"
          v-model.number="editInterval"
          type="number"
          min="1"
          max="10080"
          required
        />
        <p class="field-hint">1–10080 分钟，保存后按新间隔安排下一次检查。</p>
        <div class="modal-actions">
          <button type="button" class="button" @click="editOpen = false">取消</button
          ><button type="submit" class="button primary" :disabled="!!busy || !editName.trim()">
            保存设置
          </button>
        </div>
      </form>
    </AppDialog>
    <AppDialog v-model="deleteOpen" labelledby="delete-title">
      <div class="modal-heading">
        <h2 id="delete-title">删除这个关注？</h2>
        <button class="icon-button" aria-label="关闭" @click="deleteOpen = false">
          <X :size="18" />
        </button>
      </div>
      <p class="delete-description">「{{ monitor?.name }}」的全部快照和检查记录也会一起删除。</p>
      <div class="modal-actions">
        <button class="button" @click="deleteOpen = false">取消</button
        ><button class="button danger" :disabled="!!busy" @click="action('delete')">
          {{ busy === 'delete' ? '正在删除…' : '确认删除' }}
        </button>
      </div>
    </AppDialog>
  </div>
</template>
