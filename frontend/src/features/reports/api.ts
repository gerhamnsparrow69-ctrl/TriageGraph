import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'
import { api } from '../../api/client'
import { useLiveEvent } from '../../app/live/LiveContext'

export interface Overview {
  total: number
  open: number
  criticalOpen: number
  resolved: number
  closed: number
  slaViolations: number
  slaCompliancePercent: number | null
  mttaMinutes: number | null
  mttrMinutes: number | null
}

export interface TechnicianWorkload {
  id: number
  name: string
  role: string
  active: number
  maxLoad: number
  available: boolean
  resolvedLast7Days: number
  avgResolutionMinutes: number | null
}

export interface ServiceAnalysis {
  id: number
  name: string
  criticality: string
  incidents: number
  open: number
  health: 'OUTAGE' | 'DEGRADED' | 'HEALTHY'
  topRootCauses: string[]
}

export interface TrendDay {
  date: string
  counts: Record<string, number>
  total: number
}

export interface SlaByPriority {
  priority: string
  met: number
  breached: number
  openOnTrack: number
}

export interface AiEffectiveness {
  suggestionAcceptancePercent: number | null
  classified: number
  duplicatesJoined: number
  duplicatesMerged: number
  ticketsAvoided: number
  majorIncidents: number
}

export interface Dashboard {
  generatedAt: string
  overview: Overview
  technicians: TechnicianWorkload[]
  services: ServiceAnalysis[]
  trend: { categories: string[]; days: TrendDay[] }
  slaByPriority: SlaByPriority[]
  ai: AiEffectiveness
}

const REPORTS_KEY = ['reports'] as const

/** Refetches when anything happens on the live feed, so the numbers move during the demo. */
function useRefreshOnFeed() {
  const qc = useQueryClient()
  const refresh = useCallback(() => {
    void qc.invalidateQueries({ queryKey: REPORTS_KEY })
  }, [qc])
  useLiveEvent('feed', refresh)
}

export function useReports() {
  useRefreshOnFeed()
  return useQuery({ queryKey: [...REPORTS_KEY, 'all'], queryFn: () => api<Dashboard>('/reports'), refetchInterval: 60_000 })
}

export function useOverview() {
  useRefreshOnFeed()
  return useQuery({ queryKey: [...REPORTS_KEY, 'overview'], queryFn: () => api<Overview>('/reports/overview'), refetchInterval: 30_000 })
}

/** 75 -> "75 min", 886 -> "14.8 h", 3000 -> "2.1 d". */
export function duration(minutes: number | null): string {
  if (minutes === null) return '–'
  if (minutes < 120) return `${Math.round(minutes)} min`
  if (minutes < 48 * 60) return `${(minutes / 60).toFixed(1)} h`
  return `${(minutes / 1440).toFixed(1)} d`
}
