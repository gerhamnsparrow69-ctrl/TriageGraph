import type { RouteObject } from 'react-router'
import { assignmentTechnicianRoutes } from '../features/assignment/routes'
import { auditAdminRoutes } from '../features/audit/routes'
import { authRoutes } from '../features/auth/routes'
import { catalogAdminRoutes } from '../features/catalog/routes'
import { incidentRoutes } from '../features/incident/routes'
import { notificationRoutes } from '../features/notifications/routes'
import { reportsAdminRoutes } from '../features/reports/routes'
import { searchAdminRoutes, searchTechnicianRoutes } from '../features/search/routes'
import { settingsAdminRoutes } from '../features/settings/routes'
import { slaAdminRoutes } from '../features/sla/routes'
import { topologyAdminRoutes, topologyTechnicianRoutes } from '../features/topology/routes'
import { triageEmployeeRoutes, triageTechnicianRoutes } from '../features/triage/routes'
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
  ...triageEmployeeRoutes,
]

/** Pages inside the teal technician layout, under /technician. */
export const technicianRoutes: RouteObject[] = [
  ...notificationRoutes,
  ...incidentRoutes,
  ...triageTechnicianRoutes,
  ...assignmentTechnicianRoutes,
  ...searchTechnicianRoutes,
  ...topologyTechnicianRoutes,
]

/** Pages inside the indigo admin layout, under /admin. */
export const adminRoutes: RouteObject[] = [
  ...notificationRoutes,
  ...usersAdminRoutes,
  ...incidentRoutes,
  ...auditAdminRoutes,
  ...settingsAdminRoutes,
  ...searchAdminRoutes,
  ...reportsAdminRoutes,
  ...catalogAdminRoutes,
  ...topologyAdminRoutes,
  ...slaAdminRoutes,
]
