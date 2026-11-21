import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createBrowserRouter, RouterProvider } from 'react-router'
import HomePage from './app/HomePage'
import NotFoundPage from './app/NotFoundPage'
import { featureRoutes } from './app/routes'

const queryClient = new QueryClient()

const router = createBrowserRouter([
  { path: '/', element: <HomePage /> },
  ...featureRoutes,
  { path: '*', element: <NotFoundPage /> },
])

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}
