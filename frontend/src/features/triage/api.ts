import { api, post, put } from '../../api/client'

/**
 * Typed client for Member 2's triage endpoints (/api/v1/triage and /api/v1/admin/triage).
 * Hand-written until `npm run gen:api` regenerates src/api/schema.d.ts with these endpoints.
 */

export type ClassificationSource = 'KEYWORD' | 'KNN' | 'LLM' | 'USER' | 'FALLBACK'

export interface ClassificationSuggestion {
  categoryCode: string
  serviceId: number | null
  confidence: number
  source: ClassificationSource
  explanation: string
}

export interface Vote {
  source: string
  categoryCode: string | null
  confidence: number
  serviceName: string | null
  serviceConfidence: number
  explanation: string
}

export interface DuplicateView {
  incidentId: number
  code: string
  title: string
  status: string
  score: number
  similarity: number
  sameService: boolean
  minutesAgo: number
  serviceName: string | null
}

export interface PreviewResponse {
  suggestion: ClassificationSuggestion
  suggestedServiceName: string | null
  lowConfidence: boolean
  votes: Vote[]
  likelyDuplicates: DuplicateView[]
  possiblyRelated: DuplicateView[]
  embedder: string
}

export interface ServiceOption {
  id: number | null
  name: string
}

export interface TriageOptions {
  categories: string[]
  services: ServiceOption[]
}

export interface SimilarIncident {
  incidentId: number
  code: string
  title: string
  status: string
  score: number
  resolutionSummary: string | null
}

export interface ResolutionSuggestion {
  basedOn: SimilarIncident[]
  draft: string
  aiGenerated: boolean
}

export interface KnowledgeHit {
  id: number
  code: string
  title: string
  similarity: number
  categoryCode: string | null
  serviceName: string | null
  resolutionSummary: string | null
  rootCause: string | null
}

export interface KnowledgeSearchResponse {
  suggestion: ResolutionSuggestion
  hits: KnowledgeHit[]
}

export interface TriageSettings {
  likelyThreshold: number
  possiblyThreshold: number
  windowHours: number
  aiEnabled: boolean
  llmConfigured: boolean
  updatedAt: string
  updatedBy: number | null
}

export interface TriageStatus {
  embedder: string
  indexSize: number
  archiveEntries: number
  liveEntries: number
  openLive: number
  llm: string
  llmConfigured: boolean
  aiEnabled: boolean
}

export interface ThresholdRow {
  threshold: number
  tp: number
  fp: number
  fn: number
  tn: number
  precision: number
  recall: number
  f1: number
}

export interface PairResult {
  textA: string
  textB: string
  sameService: boolean
  duplicate: boolean
  kind: string
  cosine: number
  score: number
  predictedDuplicate: boolean
}

export interface EvaluationReport {
  embedder: string
  pairs: number
  positives: number
  atThreshold: ThresholdRow
  best: ThresholdRow
  sweep: ThresholdRow[]
  results: PairResult[]
}

export type KeywordTarget = 'CATEGORY' | 'SERVICE'

export interface KeywordRule {
  id: number
  target: KeywordTarget
  key: string
  phrase: string
}

export const triageApi = {
  preview: (body: { title: string; description: string; serviceId?: number | null }) =>
    post<PreviewResponse>('/triage/preview', body),
  options: () => api<TriageOptions>('/triage/options'),
  similar: (incidentId: number, k = 3) => api<SimilarIncident[]>(`/triage/incidents/${incidentId}/similar?k=${k}`),
  resolutionSuggestion: (incidentId: number, useAi: boolean) =>
    post<ResolutionSuggestion>(`/triage/incidents/${incidentId}/resolution-suggestion?useAi=${useAi}`),
  join: (incidentId: number) => post<void>(`/triage/incidents/${incidentId}/join`),
  merge: (incidentId: number, parentId: number) => post<void>(`/triage/incidents/${incidentId}/merge`, { parentId }),
  searchKnowledge: (query: string, useAi: boolean) =>
    post<KnowledgeSearchResponse>('/triage/knowledge/search', { query, useAi }),

  settings: () => api<TriageSettings>('/admin/triage/settings'),
  updateSettings: (s: Pick<TriageSettings, 'likelyThreshold' | 'possiblyThreshold' | 'windowHours' | 'aiEnabled'>) =>
    put<TriageSettings>('/admin/triage/settings', s),
  status: () => api<TriageStatus>('/admin/triage/status'),
  evaluation: () => api<EvaluationReport>('/admin/triage/evaluation'),
  keywords: () => api<KeywordRule[]>('/admin/triage/keywords'),
  addKeyword: (body: { target: KeywordTarget; key: string; phrase: string }) =>
    post<KeywordRule>('/admin/triage/keywords', body),
  removeKeyword: (id: number) => api<void>(`/admin/triage/keywords/${id}`, { method: 'DELETE' }),
}
