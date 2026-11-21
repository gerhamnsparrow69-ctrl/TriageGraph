import type { ComponentType } from 'react'
import type { IncidentView } from './types'

export interface IncidentPanelProps {
  incident: IncidentView
}

/**
 * Side panels other features drop into the incident page (blueprint §20): M2 adds similar incidents and
 * technician recommendations, M3 adds blast radius / SLA detail. Add ONE entry here; the page renders
 * them in order beside the main column.
 */
export const incidentSidePanels: { key: string; Component: ComponentType<IncidentPanelProps> }[] = []
