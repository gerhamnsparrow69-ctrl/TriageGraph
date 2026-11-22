import type { RouteObject } from 'react-router'

const page = () =>
  import('./TopologyPage').then((m) => ({ Component: m.default }))

export const topologyAdminRoutes: RouteObject[] = [
  { path: 'topology', lazy: page },
]

export const topologyTechnicianRoutes: RouteObject[] = [
  { path: 'topology', lazy: page },
]
