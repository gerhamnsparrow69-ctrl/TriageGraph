/** Mirrors IncidentView from the backend (incident.application.IncidentView). */

export type IncidentStatus =
  | 'NEW' | 'CLASSIFIED' | 'ASSIGNED' | 'ACKNOWLEDGED' | 'IN_PROGRESS'
  | 'PENDING_INFO' | 'RESOLVED' | 'VERIFIED' | 'CLOSED' | 'MERGED'

export type IncidentAction =
  | 'CLASSIFY' | 'ASSIGN' | 'ACKNOWLEDGE' | 'START_WORK' | 'REQUEST_INFO' | 'PROVIDE_INFO'
  | 'RESOLVE' | 'VERIFY' | 'REOPEN' | 'CLOSE' | 'MERGE' | 'ADD_NOTE' | 'FEEDBACK' | 'ESCALATE'

export type Priority = 'P1' | 'P2' | 'P3' | 'P4'

export interface IncidentNote {
  id: number
  text: string
  authorId: number | null
  /** Display name resolved by the server; null for system notes or an unknown user. */
  authorName: string | null
  internal: boolean
  createdAt: string
}

export interface IncidentView {
  id: number
  code: string
  title: string
  description: string
  status: IncidentStatus
  priority: Priority | null
  priorityScore: number | null
  priorityRationale: string | null
  impact: string
  urgency: string
  affectedUsers: number
  categoryCode: string | null
  serviceId: number | null
  classificationSource: string | null
  classificationConfidence: number | null
  reporterId: number
  reporterName: string | null
  assigneeId: number | null
  assigneeName: string | null
  assignmentStrategy: string | null
  awaitingAssignmentReason: string | null
  sla: {
    policy: string | null
    responseDueAt: string | null
    resolutionDueAt: string | null
    respondedAt: string | null
    paused: boolean
  }
  resolution: { summary: string; rootCause: string | null; resolvedAt: string; resolvedById: number | null } | null
  feedbackRating: number | null
  duplicateOfId: number | null
  reopenCount: number
  createdAt: string
  updatedAt: string
  notes: IncidentNote[]
  allowedActions: IncidentAction[]
  allowedNext: IncidentStatus[]
}
