import type { NavItem } from './nav'
import { SidebarLayout } from './SidebarLayout'
import { technicianTheme } from './themes'

const nav: NavItem[] = [
  { to: '/technician', label: 'My queue' },
  { to: '/technician/search', label: 'Search incidents' },
  { to: '/technician/topology', label: 'Service map' },
  { to: '/technician/notifications', label: 'Notifications' },
]

/** Technicians and team leads: dense teal sidebar built for working a queue. */
export default function TechnicianLayout() {
  return <SidebarLayout title="Technician console" nav={nav} theme={technicianTheme} />
}
