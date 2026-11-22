import type { AuditEntry } from './api'

/** "Sneha Rao · technician", or just the role when the user is unknown; never a raw user id. */
export function actorLabel(e: Pick<AuditEntry, 'actorId' | 'actorRole' | 'actorName'>): string {
  if (e.actorId == null) return 'System'
  const role = e.actorRole ? e.actorRole.toLowerCase().replace(/_/g, ' ') : null
  if (e.actorName) return role ? `${e.actorName} · ${role}` : e.actorName
  const fallback = role ?? 'user'
  return `${fallback.charAt(0).toUpperCase()}${fallback.slice(1)}`
}

export function transitionLabel(e: Pick<AuditEntry, 'fromState' | 'toState'>): string | null {
  const show = (s: string) => s.toLowerCase().replace(/_/g, ' ')
  if (e.fromState && e.toState) return `${show(e.fromState)} → ${show(e.toState)}`
  if (e.toState) return `→ ${show(e.toState)}`
  return null
}

export const shortHash = (h: string) => h.slice(0, 10)
