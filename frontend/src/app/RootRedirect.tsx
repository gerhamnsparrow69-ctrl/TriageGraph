import { Navigate } from 'react-router'
import { homeFor, useAuth } from '../features/auth/AuthContext'

export default function RootRedirect() {
  const { user, loading } = useAuth()
  if (loading) return null
  return <Navigate to={user ? homeFor(user.role) : '/login'} replace />
}
