import { NavLink, Outlet } from 'react-router'
import type { NavItem } from './nav'
import type { Theme } from './themes'
import { UserMenu } from './UserMenu'

/** Shared skeleton for the technician and admin consoles; each passes its own colours and menu. */
export function SidebarLayout({ title, nav, theme }: { title: string; nav: NavItem[]; theme: Theme }) {
  const item = theme.dense ? 'px-3 py-1.5 text-[13px]' : 'px-3 py-2 text-sm'
  return (
    <div className={`min-h-screen md:flex ${theme.page}`}>
      <aside className={`${theme.sidebar} md:w-60 md:min-h-screen md:sticky md:top-0 flex md:flex-col`}>
        <div className="px-4 h-14 flex items-center justify-between md:justify-start w-full md:w-auto">
          <div>
            <div className="font-bold tracking-tight">TriageGraph</div>
            <div className={`text-xs ${theme.accent}`}>{title}</div>
          </div>
          <UserMenu className="md:hidden" />
        </div>
        <nav className="hidden md:flex flex-col gap-0.5 px-2 py-2">
          {nav.map((n) => (
            <NavLink key={n.to} to={n.to} end
              className={({ isActive }) => `${item} rounded-md ${isActive ? theme.active : theme.hover}`}>
              {n.label}
            </NavLink>
          ))}
        </nav>
        <div className="hidden md:block mt-auto p-3 border-t border-white/10">
          <UserMenu />
        </div>
      </aside>
      <nav className={`md:hidden flex gap-1 overflow-x-auto px-2 py-2 ${theme.sidebar}`}>
        {nav.map((n) => (
          <NavLink key={n.to} to={n.to} end
            className={({ isActive }) => `${item} rounded-md whitespace-nowrap ${isActive ? theme.active : theme.hover}`}>
            {n.label}
          </NavLink>
        ))}
      </nav>
      <main className="flex-1 min-w-0 px-4 md:px-8 py-6">
        <Outlet />
      </main>
    </div>
  )
}
