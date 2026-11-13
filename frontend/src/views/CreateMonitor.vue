<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  ArrowRight,
  Globe2,
  ScanText,
  Clock3,
  Eye,
  LoaderCircle,
  Info,
  FileText,
  Check,
  Sparkles,
  SlidersHorizontal,
  ExternalLink
} from '@lucide/vue'
import { DEMO_URL, previewMonitor, createMonitor } from '../api/monitors'
import { refreshMonitors } from '../stores/monitors'
import { toast, message } from '../stores/notifications'
import type { Preview } from '../types'
const route = useRoute(),
  router = useRouter()
const url = ref(route.query.demo ? DEMO_URL : ''),
  name = ref(''),
  selector = ref(''),
  ignoreSelector = ref('')
const intervalMinutes = ref(360),
  advanced = ref(false),
  preview = ref<Preview | null>(null)
const previewing = ref(false),
  saving = ref(false),
  error = ref('')
const frequencies = [
  { value: 1, label: '每分钟（调试）' },
  { value: 15, label: '每 15 分钟' },
  { value: 60, label: '每小时' },
  { value: 360, label: '每 6 小时' },
  { value: 1440, label: '每天' },
  { value: 10080, label: '每周' }
]
const demo = computed(() => url.value === DEMO_URL)
watch([url, selector, ignoreSelector], () => {
  preview.value = null
  error.value = ''
})
async function getPreview() {
  if (!url.value.trim()) {
    error.value = '先输入一个完整的网页地址'
    return
  }
  previewing.value = true
  error.value = ''
  try {
    preview.value = await previewMonitor( {
      url: url.value.trim(),
      selector: selector.value,
      ignoreSelector: ignoreSelector.value
    })
    if (!name.value) name.value = preview.value.title.slice(0, 120)
  } catch (e) {
    error.value = message(e)
  } finally {
    previewing.value = false
  }
}
async function save() {
  if (!preview.value || !name.value.trim()) return
  saving.value = true
  error.value = ''
  try {
    const monitor = await createMonitor( {
      name: name.value.trim(),
      url: url.value.trim(),
      selector: selector.value,
      ignoreSelector: ignoreSelector.value,
      intervalMinutes: intervalMinutes.value
    })
    await refreshMonitors()
    toast('关注已添加，首次快照已保存')
    await router.push(`/monitors/${monitor.id}`)
  } catch (e) {
    error.value = message(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page create-page">
    <RouterLink to="/" class="back-link"><ArrowLeft :size="15" />返回关注列表</RouterLink>
    <div class="page-heading compact">
      <div>
        <div class="eyebrow">A NEW SIGNAL</div>
        <h1>添加一个值得关注的网页<span class="accent-period">。</span></h1>
        <p class="page-description">告诉页哨看哪里，剩下的交给它。</p>
      </div>
    </div>
    <div v-if="demo" class="info-banner demo-banner">
      <Sparkles :size="18" /><span
        >你正在体验演示网页。创建后，可以在详情页「发布演示更新」，观察真实的快照差异流程。</span
      >
    </div>
    <div class="create-grid">
      <form class="panel form-panel" @submit.prevent="save">
        <div class="form-section">
          <div class="form-section-heading">
            <span class="step">1</span>
            <h2>从一个网址开始</h2>
          </div>
          <label for="url">网页地址 <span class="required">*</span></label>
          <div class="input-icon">
            <Globe2 :size="17" /><input
              id="url"
              v-model="url"
              type="url"
              required
              maxlength="2048"
              placeholder="https://example.com/changelog"
              :disabled="saving || previewing"
            />
          </div>
          <p class="field-hint">支持无需登录即可访问的静态网页。</p>
          <div class="form-field">
            <label for="name">关注名称 <span class="required">*</span></label
            ><input
              id="name"
              v-model="name"
              required
              maxlength="120"
              placeholder="例如：Spring 版本更新"
              :disabled="saving"
            />
            <p class="field-hint">预览后自动填入网页标题，也可以自己命名。</p>
          </div>
        </div>
        <div class="form-section">
          <div class="form-section-heading">
            <span class="step">2</span>
            <h2>选择关注的内容</h2>
          </div>
          <div class="extraction-option">
            <span class="option-check"><Check :size="13" /></span>
            <div>
              <strong>智能提取正文</strong>
              <p>优先读取 main / article，自动忽略导航、页脚和脚本。</p>
            </div>
            <ScanText :size="20" />
          </div>
          <button
            type="button"
            class="advanced-toggle"
            :aria-expanded="advanced"
            @click="advanced = !advanced"
          >
            <SlidersHorizontal :size="14" />{{
              advanced ? '收起高级设置' : '高级设置：指定或忽略区域'
            }}
          </button>
          <div v-if="advanced" class="advanced-fields">
            <label for="selector">关注区域（CSS 选择器）</label
            ><input
              id="selector"
              v-model="selector"
              maxlength="500"
              placeholder="例如：main、.release-list、#content"
              :disabled="saving || previewing"
            />
            <p class="field-hint">留空时自动提取正文。</p>
            <label for="ignore">忽略区域（CSS 选择器）</label
            ><input
              id="ignore"
              v-model="ignoreSelector"
              maxlength="500"
              placeholder="例如：.timestamp, .view-count"
              :disabled="saving || previewing"
            />
            <p class="field-hint">排除时间、浏览量等不需要关注的内容。</p>
          </div>
        </div>
        <div class="form-section">
          <div class="form-section-heading">
            <span class="step">3</span>
            <h2>设置检查频率</h2>
          </div>
          <label for="frequency">多久检查一次</label>
          <div class="input-icon">
            <Clock3 :size="16" /><select
              id="frequency"
              v-model="intervalMinutes"
              :disabled="saving"
            >
              <option v-for="f in frequencies" :key="f.value" :value="f.value">
                {{ f.label }}
              </option>
            </select>
          </div>
          <p class="field-hint">选择适合网页更新节奏的频率。</p>
        </div>
        <div v-if="error" class="inline-error" role="alert"><Info :size="16" />{{ error }}</div>
        <div class="form-actions">
          <button
            type="button"
            class="button"
            :disabled="previewing || saving || !url.trim()"
            @click="getPreview"
          >
            <LoaderCircle v-if="previewing" class="spin" :size="16" /><Eye v-else :size="16" />{{
              previewing ? '正在读取…' : '预览正文'
            }}</button
          ><button
            class="button primary"
            type="submit"
            :disabled="!preview || !name.trim() || saving || previewing"
          >
            <LoaderCircle v-if="saving" class="spin" :size="16" />{{
              saving ? '正在创建…' : '创建关注'
            }}<ArrowRight v-if="!saving" :size="16" />
          </button>
        </div>
        <p class="form-bottom-hint">先预览正文，确认关注区域后即可创建。</p>
      </form>
      <section class="panel preview-panel" aria-label="正文预览">
        <div class="preview-toolbar">
          <span class="window-dots"><i /><i /><i /></span><span>正文预览</span
          ><span v-if="preview" class="preview-ready"><span class="tiny-dot" />提取完成</span>
        </div>
        <div v-if="previewing" class="preview-placeholder">
          <LoaderCircle class="spin" :size="30" />
          <h3>正在读取这个网页</h3>
          <p>提取正文，过滤无关内容…</p>
        </div>
        <template v-else-if="preview"
          ><div class="preview-heading">
            <span class="eyebrow">CONTENT PREVIEW</span>
            <h2>{{ preview.title }}</h2>
            <div class="preview-counts">
              <span>{{ preview.characters.toLocaleString() }} 字符</span
              ><span>{{ preview.lines }} 行正文</span
              ><a v-if="!demo" :href="preview.resolvedUrl" target="_blank" rel="noopener noreferrer"
                >打开原网页 <ExternalLink :size="12"
              /></a>
            </div>
          </div>
          <pre class="preview-content">{{ preview.content }}</pre>
        </template>
        <div v-else class="preview-placeholder">
          <span class="placeholder-icon"><FileText :size="31" :stroke-width="1.4" /></span>
          <h3>先看看，页哨会关注什么</h3>
          <p>输入网址后点击「预览正文」，<br />这里会显示提取后的内容。</p>
          <div class="preview-skeleton"><span /><span /><span /><span /></div>
        </div>
        <div class="preview-bottom"><Info :size="14" />仅显示提取后的文本，不加载网页脚本。</div>
      </section>
    </div>
  </div>
</template>
