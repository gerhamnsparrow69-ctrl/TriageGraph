import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createBrowserRouter, Outlet, RouterProvider } from 'react-router'
import ComingSoon from './app/ComingSoon'
import { ForbiddenPage, NotFoundPage } from './app/ErrorPages'
import { AdminHome, EmployeeHome, TechnicianHome } from './app/homes'
import AdminLayout from './app/layouts/AdminLayout'
import EmployeeLayout from './app/layouts/EmployeeLayout'
import TechnicianLayout from './app/layouts/TechnicianLayout'
import RootRedirect from './app/RootRedirect'
import { adminRoutes, employeeRoutes, publicRoutes, technicianRoutes } from './app/routes'
import { ToastProvider } from './app/ui/Toasts'
import { AuthProvider } from './features/auth/AuthContext'
import { RequireRole } from './features/auth/RequireRole'

const queryClient = new QueryClient({
  defaultOptions: { queries: { refetchOnWindowFocus: false } },
})

function Providers() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  )
}

const router = createBrowserRouter([
  {
    element: <Providers />,
    children: [
      { path: '/', element: <RootRedirect /> },
      ...publicRoutes,
      {
        path: '/employee',
        element: <RequireRole roles={['EMPLOYEE']}><EmployeeLayout /></RequireRole>,
        children: [{ index: true, element: <EmployeeHome /> }, ...employeeRoutes, { path: '*', element: <ComingSoon /> }],
      },
      {
        path: '/technician',
        element: <RequireRole roles={['TECHNICIAN', 'TEAM_LEAD']}><TechnicianLayout /></RequireRole>,
        children: [{ index: true, element: <TechnicianHome /> }, ...technicianRoutes, { path: '*', element: <ComingSoon /> }],
      },
      {
        path: '/admin',
        element: <RequireRole roles={['ADMIN', 'IT_MANAGER']}><AdminLayout /></RequireRole>,
        children: [{ index: true, element: <AdminHome /> }, ...adminRoutes, { path: '*', element: <ComingSoon /> }],
      },
      { path: '/403', element: <ForbiddenPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <RouterProvider router={router} />
      </ToastProvider>
    </QueryClientProvider>
  )
}
