import type { RouteObject } from 'react-router'
import AuditPage from './AuditPage'

/** Admin layout only: /admin/audit (the sidebar link already exists). */
export const auditAdminRoutes: RouteObject[] = [{ path: 'audit', element: <AuditPage /> }]
