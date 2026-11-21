import { api, post, put } from '../../api/client'

/** Typed client for Member 2's assignment endpoints (/api/v1/assignment and /api/v1/admin/assignment). */

export type Priority = 'P1' | 'P2' | 'P3' | 'P4'

export interface TechnicianRecommendation {
  technicianId: number
  name: string
  score: number
  strategyKey: string
  reasons: string[]
}

export interface StrategyView {
  key: string
  displayName: string
  description: string
  active: boolean
}

export interface AssignmentSettings {
  activeStrategy: string
  strategies: StrategyView[]
  updatedAt: string
  updatedBy: number | null
}

export interface StrategyOutcome {
  strategyKey: string
  displayName: string
  active: boolean
  pick: TechnicianRecommendation | null
  error: string | null
}

export interface QueueItem {
  incidentId: number
  code: string
  title: string
  status: string
  priority: Priority | null
  createdAt: string | null
  responseDueAt: string | null
  resolutionDueAt: string | null
}

export interface TechnicianWorkload {
  technicianId: number
  name: string
  skills: string[]
  activeLoad: number
  maxLoad: number
  loadPercent: number
  available: boolean
  atCapacity: boolean
  queue: QueueItem[]
  incidentDataAvailable: boolean
}

export interface RecommendationQuery {
  categoryCode: string
  serviceId: number | null
  priority: Priority
  topK?: number
  strategyKey?: string
}

export const assignmentApi = {
  strategies: () => api<StrategyView[]>('/assignment/strategies'),
  recommend: (q: RecommendationQuery) => post<TechnicianRecommendation[]>('/assignment/recommendations', q),
  compare: (q: RecommendationQuery) => post<StrategyOutcome[]>('/assignment/compare', q),
  forIncident: (incidentId: number, topK = 3) =>
    api<TechnicianRecommendation[]>(`/assignment/incidents/${incidentId}/recommendations?topK=${topK}`),
  reassign: (incidentId: number, body: { technicianId: number; reason: string; override: boolean }) =>
    post<void>(`/assignment/incidents/${incidentId}/reassign`, body),
  myWorkload: () => api<TechnicianWorkload>('/assignment/workload/me'),
  teamWorkload: () => api<TechnicianWorkload[]>('/assignment/workload'),
  settings: () => api<AssignmentSettings>('/admin/assignment/settings'),
  switchStrategy: (strategyKey: string) => put<AssignmentSettings>('/admin/assignment/settings', { strategyKey }),
}
