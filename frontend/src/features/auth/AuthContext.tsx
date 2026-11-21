import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, type ReactNode } from 'react'
import { api, post, setUnauthorizedHandler, tokenStore, type Role, type Schemas, type User } from '../../api/client'

interface AuthState {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<User>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient()

  const me = useQuery({
    queryKey: ['me'],
    queryFn: () => api<User>('/auth/me'),
    enabled: tokenStore.get() !== null,
    retry: false,
    staleTime: Infinity,
  })

  const logout = useCallback(() => {
    tokenStore.set(null)
    qc.setQueryData(['me'], null)
    qc.clear()
  }, [qc])

  useEffect(() => setUnauthorizedHandler(logout), [logout])

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await post<Schemas['LoginResponse']>('/auth/login', { email, password })
      tokenStore.set(res.token ?? null)
      qc.setQueryData(['me'], res.user)
      return res.user as User
    },
    [qc],
  )

  const value = useMemo<AuthState>(
    () => ({
      user: tokenStore.get() ? (me.data ?? null) : null,
      loading: tokenStore.get() !== null && me.isPending,
      login,
      logout,
    }),
    [me.data, me.isPending, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

/** Where each role lands after login. */
export function homeFor(role: Role | undefined): string {
  switch (role) {
    case 'EMPLOYEE':
      return '/employee'
    case 'TECHNICIAN':
    case 'TEAM_LEAD':
      return '/technician'
    case 'ADMIN':
    case 'IT_MANAGER':
      return '/admin'
    default:
      return '/login'
  }
}
