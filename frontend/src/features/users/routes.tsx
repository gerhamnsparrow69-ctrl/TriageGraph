import type { RouteObject } from 'react-router'
import TechniciansPage from './TechniciansPage'
import UsersPage from './UsersPage'

/** /admin/users and /admin/technicians */
export const usersAdminRoutes: RouteObject[] = [
  { path: 'users', element: <UsersPage /> },
  { path: 'technicians', element: <TechniciansPage /> },
]
