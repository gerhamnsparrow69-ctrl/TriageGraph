import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api, ApiError, tokenStore } from '../../api/client'

export type SlaStatus = 'ON_TRACK' | 'AT_RISK' | 'BREACHED' | 'MET' | 'PAUSED'

export interface IncidentRow {
  id: number
  code: string
  title: string
  status: string
  priority: string | null
  categoryCode: string | null
  serviceId: number | null
  serviceName: string | null
  reporterId: number
  reporterName: string | null
  assigneeId: number | null
  assigneeName: string | null
  slaStatus: SlaStatus
  resolutionDueAt: string | null
  major: boolean
  createdAt: string
  resolvedAt: string | null
}

export interface SearchPage {
  items: IncidentRow[]
  page: number
  size: number
  total: number
}

export interface Option {
  value: string
  label: string
}

export interface SearchOptions {
  services: Option[]
  categories: string[]
  technicians: Option[]
}

/** The 11 criteria plus sort. Empty strings and empty lists mean "any". */
export interface Filters {
  code: string
  from: string
  to: string
  status: string[]
  priority: string[]
  category: string
  serviceId: string
  technicianId: string
  slaStatus: string
  reporterId: string
  major: string
  q: string
  sort: string
}

export const emptyFilters: Filters = {
  code: '',
  from: '',
  to: '',
  status: [],
  priority: [],
  category: '',
  serviceId: '',
  technicianId: '',
  slaStatus: '',
  reporterId: '',
  major: '',
  q: '',
  sort: 'newest',
}

export function toParams(f: Filters, page?: number, size?: number): URLSearchParams {
  const p = new URLSearchParams()
  for (const [key, value] of Object.entries(f)) {
    if (Array.isArray(value)) value.forEach((v) => p.append(key, v))
    else if (value) p.set(key, value)
  }
  if (page !== undefined) p.set('page', String(page))
  if (size !== undefined) p.set('size', String(size))
  return p
}

export function useIncidentSearch(filters: Filters, page: number, size = 20) {
  const params = toParams(filters, page, size).toString()
  return useQuery({
    queryKey: ['search', 'incidents', params],
    queryFn: () => api<SearchPage>(`/search/incidents?${params}`),
    placeholderData: keepPreviousData,
  })
}

export const useSearchOptions = () =>
  useQuery({ queryKey: ['search', 'options'], queryFn: () => api<SearchOptions>('/search/incidents/options'), staleTime: 300_000 })

/** Downloads every match as CSV. fetch + blob because a plain link can't send the bearer token. */
export async function downloadCsv(filters: Filters): Promise<void> {
  const token = tokenStore.get()
  const res = await fetch(`/api/v1/search/incidents/export?${toParams(filters).toString()}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!res.ok) throw new ApiError({ status: res.status, detail: `Export failed (${res.status})` })
  const name = /filename="([^"]+)"/.exec(res.headers.get('Content-Disposition') ?? '')?.[1] ?? 'incidents.csv'
  const url = URL.createObjectURL(await res.blob())
  const a = document.createElement('a')
  a.href = url
  a.download = name
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
