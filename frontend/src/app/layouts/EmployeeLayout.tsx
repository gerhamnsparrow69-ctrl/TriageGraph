import { NavLink, Outlet } from 'react-router'
import type { NavItem } from './nav'
import { UserMenu } from './UserMenu'

const nav: NavItem[] = [
  { to: '/employee', label: 'My incidents' },
  { to: '/employee/report', label: 'Report an issue' },
  { to: '/employee/notifications', label: 'Notifications' },
]

/** Employees get a simple, friendly top bar in blue. */
export default function EmployeeLayout() {
  return (
    <div className="min-h-screen bg-blue-50/40">
      <header className="bg-blue-600 text-white">
        <div className="max-w-5xl mx-auto px-4 h-14 flex items-center gap-6">
          <span className="font-bold tracking-tight">TriageGraph</span>
          <nav className="flex gap-1 overflow-x-auto">
            {nav.map((n) => (
              <NavLink key={n.to} to={n.to} end
                className={({ isActive }) =>
                  `px-3 py-1.5 rounded-md text-sm whitespace-nowrap ${isActive ? 'bg-white/20' : 'hover:bg-white/10'}`}>
                {n.label}
              </NavLink>
            ))}
          </nav>
          <UserMenu className="ml-auto" />
        </div>
      </header>
      <main className="max-w-5xl mx-auto px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
