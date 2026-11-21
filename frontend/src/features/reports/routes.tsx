import type { RouteObject } from 'react-router'

/** /admin/reports, loaded on demand so the chart library stays out of the main bundle. */
export const reportsAdminRoutes: RouteObject[] = [
  { path: 'reports', lazy: () => import('./ReportsPage').then((m) => ({ Component: m.default })) },
]
