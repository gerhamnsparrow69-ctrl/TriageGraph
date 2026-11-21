import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createBrowserRouter, Outlet, RouterProvider } from 'react-router'
import ComingSoon from './app/ComingSoon'
import { ForbiddenPage, NotFoundPage } from './app/ErrorPages'
import { EmployeeHome, TechnicianHome } from './app/homes'
import AdminLayout from './app/layouts/AdminLayout'
import EmployeeLayout from './app/layouts/EmployeeLayout'
import TechnicianLayout from './app/layouts/TechnicianLayout'
import RootRedirect from './app/RootRedirect'
import { adminRoutes, employeeRoutes, publicRoutes, technicianRoutes } from './app/routes'
import { ToastProvider } from './app/ui/Toasts'
import { LiveProvider } from './app/live/LiveProvider'
import { AuthProvider } from './features/auth/AuthContext'
import { RequireRole } from './features/auth/RequireRole'
import CommandCentre from './features/dashboard/CommandCentre'
import { useNotificationStream } from './features/notifications/useNotificationStream'

const queryClient = new QueryClient({
  defaultOptions: { queries: { refetchOnWindowFocus: false } },
})

function StreamListeners() {
  useNotificationStream()
  return null
}

function Providers() {
  return (
    <AuthProvider>
      <LiveProvider>
        <StreamListeners />
        <Outlet />
      </LiveProvider>
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
        children: [{ index: true, element: <CommandCentre /> }, ...adminRoutes, { path: '*', element: <ComingSoon /> }],
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
