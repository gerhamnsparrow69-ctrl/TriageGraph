import type { RouteObject } from 'react-router'
import NotificationsPage from './NotificationsPage'

/** Mounted inside every layout, so each role has /<role>/notifications. */
export const notificationRoutes: RouteObject[] = [{ path: 'notifications', element: <NotificationsPage /> }]
