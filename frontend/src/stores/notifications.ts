import { ref } from 'vue'
export const toasts = ref<{ id: number; text: string; error: boolean }[]>([])
let toastId = 0
export function toast(text: string, error = false) {
  const id = ++toastId
  toasts.value.push({ id, text, error })
  setTimeout(() => {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }, 5000)
}
export const message = (e: unknown) => (e instanceof Error ? e.message : '操作失败，请稍后重试')
