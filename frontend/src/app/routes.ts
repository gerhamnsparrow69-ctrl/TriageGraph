import type { RouteObject } from 'react-router'
import { auditAdminRoutes } from '../features/audit/routes'
import { authRoutes } from '../features/auth/routes'
import { incidentRoutes } from '../features/incident/routes'
import { notificationRoutes } from '../features/notifications/routes'
import { usersAdminRoutes } from '../features/users/routes'

/**
 * Feature route registry (blueprint §22, rule 3). Each feature exports arrays from
 * src/features/<feature>/routes.tsx and adds ONE spread line to the list it belongs in.
 * Paths inside a layout list are relative to that layout (e.g. 'reports' → /admin/reports).
 */

/** Pages outside any layout (login, register). */
export const publicRoutes: RouteObject[] = [
  ...authRoutes,
]

/** Pages inside the blue employee layout, under /employee. */
export const employeeRoutes: RouteObject[] = [
  ...notificationRoutes,
  ...incidentRoutes,
]

/** Pages inside the teal technician layout, under /technician. */
export const technicianRoutes: RouteObject[] = [
  ...notificationRoutes,
  ...incidentRoutes,
]

/** Pages inside the indigo admin layout, under /admin. */
export const adminRoutes: RouteObject[] = [
  ...notificationRoutes,
  ...usersAdminRoutes,
  ...incidentRoutes,
  ...auditAdminRoutes,
]
