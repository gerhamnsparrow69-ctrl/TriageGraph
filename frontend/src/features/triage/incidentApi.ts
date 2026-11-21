import { post } from '../../api/client'

/**
 * Submitting a new incident is Member 1's endpoint (POST /api/v1/incidents, IncidentRequests.Report).
 * categoryCode and serviceId are sent only when the reporter changed the suggestion; otherwise they are
 * null and the incident module asks triage to classify the text, so the real source (KNN, KEYWORD, LLM)
 * is recorded instead of "USER".
 */

export type Impact = 'MINOR' | 'MODERATE' | 'SIGNIFICANT' | 'EXTENSIVE'
export type Urgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface NewIncident {
  title: string
  description: string
  impact: Impact
  urgency: Urgency
  affectedUsers: number
  categoryCode: string | null
  serviceId: number | null
}

/** The incident module answers with its full IncidentView; the report page only needs these. */
export interface CreatedIncident {
  id: number
  code: string
  status: string
  assigneeId: number | null
}

export const submitIncident = (body: NewIncident) => post<CreatedIncident>('/incidents', body)
