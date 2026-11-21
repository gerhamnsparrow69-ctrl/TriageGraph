import type { RouteObject } from 'react-router'
import KnowledgeSearchPage from './KnowledgeSearchPage'
import ReportIncidentPage from './ReportIncidentPage'
import ResolutionAssistantPage from './ResolutionAssistantPage'

/** /employee/report */
export const triageEmployeeRoutes: RouteObject[] = [{ path: 'report', element: <ReportIncidentPage /> }]

/** /technician/knowledge and /technician/assist/:incidentId */
export const triageTechnicianRoutes: RouteObject[] = [
  { path: 'knowledge', element: <KnowledgeSearchPage /> },
  { path: 'assist/:incidentId', element: <ResolutionAssistantPage /> },
]
