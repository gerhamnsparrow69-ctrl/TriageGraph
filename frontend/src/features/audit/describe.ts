import type { AuditEntry } from './api'

export function actorLabel(e: Pick<AuditEntry, 'actorId' | 'actorRole'>): string {
  if (e.actorId == null) return 'System'
  const role = e.actorRole ? e.actorRole.toLowerCase().replace(/_/g, ' ') : 'user'
  return `${role.charAt(0).toUpperCase()}${role.slice(1)} #${e.actorId}`
}

export function transitionLabel(e: Pick<AuditEntry, 'fromState' | 'toState'>): string | null {
  const show = (s: string) => s.toLowerCase().replace(/_/g, ' ')
  if (e.fromState && e.toState) return `${show(e.fromState)} → ${show(e.toState)}`
  if (e.toState) return `→ ${show(e.toState)}`
  return null
}

export const shortHash = (h: string) => h.slice(0, 10)
