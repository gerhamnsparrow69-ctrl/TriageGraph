import type { RouteObject } from 'react-router'
import SlaPoliciesPage from './SlaPoliciesPage'

export const slaAdminRoutes: RouteObject[] = [
  { path: 'sla', element: <SlaPoliciesPage /> },
]
