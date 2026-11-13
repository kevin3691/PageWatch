import type { Detail, Monitor, Preview, SnapshotDetail } from '../types'
import { api, post } from './request'

export const DEMO_URL = 'https://demo.pagewatch.test/changelog'
export const listMonitors = () => api<Monitor[]>('/monitors')
export const getMonitor = (id: string | string[]) => api<Detail>(`/monitors/${id}`)
export const getSnapshot = (monitorId: string | string[], id: string) =>
  api<SnapshotDetail>(`/monitors/${monitorId}/snapshots/${id}`)
export const previewMonitor = (request: { url: string; selector: string; ignoreSelector: string }) =>
  post<Preview>('/preview', request)
export const createMonitor = (request: {
  name: string
  url: string
  selector: string
  ignoreSelector: string
  intervalMinutes: number
}) => post<Monitor>('/monitors', request)
export const updateMonitor = (id: string, request: { name: string; intervalMinutes: number; active: boolean }) =>
  api<Monitor>(`/monitors/${id}`, { method: 'PATCH', body: JSON.stringify(request) })
export const deleteMonitor = (id: string) => api<void>(`/monitors/${id}`, { method: 'DELETE' })
export const markRead = (id: string) => post<void>(`/monitors/${id}/read`)
export const checkMonitor = (id: string) => post<{ accepted: boolean }>(`/monitors/${id}/check`)
export const checkAllMonitors = () => post<{ accepted: number }>('/checks')
export const advanceDemo = () => post<{ version: number }>('/demo/advance')
