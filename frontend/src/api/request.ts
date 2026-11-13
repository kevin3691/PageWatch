export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), 35000)
  try {
    const response = await fetch(`/api${path}`, {
      ...options,
      signal: options.signal ?? controller.signal,
      headers: { 'Content-Type': 'application/json', ...options.headers }
    })
    if (!response.ok) {
      const body = await response.json().catch(() => ({}))
      throw new Error(body.message ?? `请求失败（HTTP ${response.status}）`)
    }
    return response.status === 204 ? (undefined as T) : ((await response.json()) as T)
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError')
      throw new Error('请求超时，请稍后重试')
    if (e instanceof TypeError) throw new Error('连接不上后端，请确认服务已经启动')
    throw e
  } finally {
    clearTimeout(timer)
  }
}
export const post = <T>(path: string, body?: unknown) =>
  api<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })
