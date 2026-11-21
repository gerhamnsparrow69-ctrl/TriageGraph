import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, post } from '../../api/client'
import type { IncidentView } from './types'

export const incidentKeys = {
  detail: (id: number) => ['incident', id] as const,
  technicians: ['incident', 'technicians'] as const,
}

export const useIncident = (id: number) =>
  useQuery({ queryKey: incidentKeys.detail(id), queryFn: () => api<IncidentView>(`/incidents/${id}`), enabled: id > 0 })

/** Any lifecycle action. The response is the updated incident, so the cache is replaced, not refetched. */
export function useIncidentAction(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ path, body }: { path: string; body?: unknown }) => post<IncidentView>(`/incidents/${id}/${path}`, body),
    onSuccess: (incident) => {
      qc.setQueryData(incidentKeys.detail(id), incident)
      qc.invalidateQueries({ queryKey: ['audit', 'timeline', id] })
    },
  })
}

export interface TechnicianOption {
  id: number
  name: string
  technician?: { skills: string[]; available: boolean } | null
}

/** For the reassign picker (team leads and admins only, like the endpoint). */
export const useTechnicians = (enabled: boolean) =>
  useQuery({
    queryKey: incidentKeys.technicians,
    queryFn: () => api<TechnicianOption[]>('/admin/technicians'),
    enabled,
    staleTime: 60_000,
  })
