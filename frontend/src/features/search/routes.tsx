import type { RouteObject } from 'react-router'
import SearchPage from './SearchPage'

/** /admin/incidents: every incident, for administrators and the IT manager. */
export const searchAdminRoutes: RouteObject[] = [{ path: 'incidents', element: <SearchPage /> }]

/** /technician/search: a technician's own incidents, or a team lead's team. */
export const searchTechnicianRoutes: RouteObject[] = [{ path: 'search', element: <SearchPage /> }]
