export interface Monitor {
  id: string
  name: string
  url: string
  selector: string
  ignoreSelector: string
  intervalMinutes: number
  active: boolean
  unread: boolean
  createdAt: string
  lastCheckedAt: string | null
  nextCheckAt: string
  lastChangeAt: string | null
  lastError: string | null
  snapshotCount: number
  checking: boolean
}
export interface Snapshot {
  id: string
  monitorId: string
  previousId: string | null
  createdAt: string
  addedCount: number
  removedCount: number
}
export interface CheckRun {
  id: string
  checkedAt: string
  outcome: 'BASELINE' | 'CHANGED' | 'UNCHANGED' | 'FAILED'
  durationMs: number
  error: string | null
}
export interface Detail {
  monitor: Monitor
  snapshots: Snapshot[]
  checks: CheckRun[]
}
export interface DiffLine {
  kind: 'add' | 'remove' | 'equal'
  text: string
  oldLine: number | null
  newLine: number | null
}
export interface SnapshotDetail {
  snapshot: Snapshot
  content: string
  previousContent: string
  diff: DiffLine[]
}
export interface Preview {
  title: string
  content: string
  characters: number
  lines: number
  resolvedUrl: string
}
