import { Link } from 'react-router'
import { useAuth } from '../features/auth/AuthContext'
import { PageHeader } from './PageHeader'

export function EmployeeHome() {
  const { user } = useAuth()
  return (
    <>
      <PageHeader
        title={`Hi ${user?.name?.split(' ')[0] ?? ''}`}
        subtitle="Your reported incidents and their status."
        actions={
          <Link to="/employee/report" className="rounded-md bg-blue-600 text-white px-4 py-2 text-sm font-medium hover:bg-blue-700">
            Report an issue
          </Link>
        }
      />
      <div className="rounded-xl bg-white ring-1 ring-slate-200 p-8 text-center text-slate-500 text-sm">
        You haven't reported anything yet.
      </div>
    </>
  )
}

export function TechnicianHome() {
  return (
    <>
      <PageHeader title="My queue" subtitle="Incidents assigned to you, most urgent first." />
      <div className="rounded-lg bg-white ring-1 ring-slate-200 p-6 text-slate-500 text-sm">Nothing assigned right now.</div>
    </>
  )
}

export function AdminHome() {
  const tiles = ['Open incidents', 'Critical open', 'SLA compliance', 'MTTR']
  return (
    <>
      <PageHeader title="Command Centre" subtitle="Live view of the service desk." />
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {tiles.map((t) => (
          <div key={t} className="rounded-xl bg-white ring-1 ring-slate-200 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">{t}</div>
            <div className="text-2xl font-semibold text-slate-300 mt-1">–</div>
          </div>
        ))}
      </div>
    </>
  )
}
