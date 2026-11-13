import { clockNow } from '../stores/monitors'
export function dateTime(value: string | null) {
  return value
    ? new Intl.DateTimeFormat('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      }).format(new Date(value))
    : '尚未检查'
}
export function relative(value: string | null) {
  if (!value) return '尚未检查'
  const diff = clockNow.value - new Date(value).getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return `${Math.floor(diff / 86400000)} 天前`
}
export function intervalLabel(minutes: number) {
  if (minutes % 1440 === 0) return `${minutes / 1440} 天`
  if (minutes % 60 === 0) return `${minutes / 60} 小时`
  return `${minutes} 分钟`
}
export function hostname(url: string) {
  try {
    return new URL(url).hostname
  } catch {
    return url
  }
}
