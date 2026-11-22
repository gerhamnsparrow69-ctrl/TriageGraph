import { api } from '../../api/client'

export type Criticality = 'LOW' | 'MEDIUM' | 'HIGH' | 'MISSION_CRITICAL'

export interface ServiceRef {
  id: number
  code: string
  name: string
  criticality: Criticality
}

export interface CategoryRef {
  code: string
  name: string
  defaultSkill: string | null
}

export const catalogApi = {
  services: () => api<ServiceRef[]>('/catalog/services'),
  service: (id: number) => api<ServiceRef>(`/catalog/services/${id}`),
  categories: () => api<CategoryRef[]>('/catalog/categories'),
  category: (code: string) => api<CategoryRef>(`/catalog/categories/${encodeURIComponent(code)}`),
}
