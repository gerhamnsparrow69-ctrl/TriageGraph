import type { NavItem } from './nav'
import { SidebarLayout } from './SidebarLayout'
import { adminTheme } from './themes'

const nav: NavItem[] = [
  { to: '/admin', label: 'Command Centre' },
  { to: '/admin/incidents', label: 'Incidents' },
  { to: '/admin/reports', label: 'Reports' },
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/technicians', label: 'Technicians' },
  { to: '/admin/catalog', label: 'Service catalog' },
  { to: '/admin/topology', label: 'Dependency graph' },
  { to: '/admin/sla', label: 'SLA policies' },
  { to: '/admin/audit', label: 'Audit trail' },
  { to: '/admin/notifications', label: 'Notifications' },
  { to: '/admin/settings', label: 'Settings' },
]

/** Admins and the IT manager: full dark-indigo sidebar. */
export default function AdminLayout() {
  return <SidebarLayout title="Administration" nav={nav} theme={adminTheme} />
}
