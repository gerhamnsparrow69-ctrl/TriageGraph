import { useNavigate } from 'react-router'
import { useAuth } from '../../features/auth/AuthContext'

const roleLabel: Record<string, string> = {
  EMPLOYEE: 'Employee',
  TECHNICIAN: 'Technician',
  TEAM_LEAD: 'Team lead',
  ADMIN: 'Administrator',
  IT_MANAGER: 'IT manager',
}

export function UserMenu({ className = '' }: { className?: string }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!user) return null
  return (
    <div className={`flex items-center gap-3 text-sm ${className}`}>
      <div className="text-right leading-tight hidden sm:block">
        <div className="font-medium">{user.name}</div>
        <div className="opacity-70 text-xs">{roleLabel[user.role ?? ''] ?? user.role}</div>
      </div>
      <button
        onClick={() => {
          logout()
          navigate('/login', { replace: true })
        }}
        className="rounded-md px-3 py-1.5 bg-white/10 hover:bg-white/20 ring-1 ring-white/20"
      >
        Sign out
      </button>
    </div>
  )
}
