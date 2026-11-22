import type { RouteObject } from 'react-router'
import TopologyPage from './TopologyPage'

export const topologyAdminRoutes: RouteObject[] = [
  { path: 'topology', element: <TopologyPage /> },
]

export const topologyTechnicianRoutes: RouteObject[] = [
  { path: 'topology', element: <TopologyPage /> },
]
