import type { RouteObject } from 'react-router'
import IncidentDetailPage from './IncidentDetailPage'

/** Mounted inside every layout: /employee/incidents/:id, /technician/incidents/:id, /admin/incidents/:id. */
export const incidentRoutes: RouteObject[] = [{ path: 'incidents/:id', element: <IncidentDetailPage /> }]
