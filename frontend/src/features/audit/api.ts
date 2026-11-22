import { useMutation, useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'

/** Mirrors AuditEntryView. */
export interface AuditEntry {
  id: number
  occurredAt: string
  actorId: number | null
  actorRole: string | null
  /** Display name resolved by the server; null for system entries or an unknown user. */
  actorName: string | null
  action: string
  incidentId: number | null
  incidentCode: string | null
  fromState: string | null
  toState: string | null
  description: string | null
  prevHash: string
  hash: string
}

export interface AuditVerification {
  valid: boolean
  entriesChecked: number
  firstBrokenId: number | null
  reason: string | null
  message: string
}

export const AUDIT_PAGE_SIZE = 50

export const useTimeline = (incidentId: number) =>
  useQuery({
    queryKey: ['audit', 'timeline', incidentId],
    queryFn: () => api<AuditEntry[]>(`/incidents/${incidentId}/timeline`),
  })

export const useAuditPage = (page: number) =>
  useQuery({
    queryKey: ['audit', 'page', page],
    queryFn: () => api<AuditEntry[]>(`/admin/audit?page=${page}&size=${AUDIT_PAGE_SIZE}`),
    placeholderData: (previous) => previous,
  })

/** Verification walks the whole chain, so it only runs when the admin presses the button. */
export const useVerifyAudit = () =>
  useMutation({ mutationFn: () => api<AuditVerification>('/admin/audit/verify') })
