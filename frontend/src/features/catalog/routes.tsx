import type { RouteObject } from 'react-router'
import CatalogPage from './CatalogPage'

export const catalogAdminRoutes: RouteObject[] = [
  { path: 'catalog', element: <CatalogPage /> },
]
