import type { ComponentType } from 'react'
import IncidentAssignmentPanel from '../assignment/IncidentAssignmentPanel'
import IncidentTriagePanel from '../triage/IncidentTriagePanel'
import type { IncidentView } from './types'

export interface IncidentPanelProps {
  incident: IncidentView
}

/**
 * Side panels other features drop into the incident page (blueprint §20): M2 adds similar incidents and
 * technician recommendations, M3 adds blast radius / SLA detail. Add ONE entry here; the page renders
 * them in order beside the main column.
 */
export const incidentSidePanels: { key: string; Component: ComponentType<IncidentPanelProps> }[] = [
  { key: 'm2-assignment', Component: IncidentAssignmentPanel },
  { key: 'm2-triage', Component: IncidentTriagePanel },
]
