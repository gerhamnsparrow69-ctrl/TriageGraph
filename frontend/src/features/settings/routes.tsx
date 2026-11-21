import type { RouteObject } from 'react-router'

/** /admin/settings, loaded on demand so the chart library is not in the main bundle. */
export const settingsAdminRoutes: RouteObject[] = [
  { path: 'settings', lazy: () => import('./SettingsPage').then((m) => ({ Component: m.default })) },
]
