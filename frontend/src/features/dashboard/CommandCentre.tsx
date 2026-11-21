import { Link } from 'react-router'
import { PageHeader } from '../../app/PageHeader'
import { useAuth } from '../auth/AuthContext'
import { duration, useOverview } from '../reports/api'
import { LiveFeedPanel } from './LiveFeedPanel'
import { TestNotificationCard } from './TestNotificationCard'

export default function CommandCentre() {
  const { user } = useAuth()
  const overview = useOverview()
  const o = overview.data
  const tiles: { label: string; value: string; alert?: boolean }[] = [
    { label: 'Open incidents', value: o ? String(o.open) : '–' },
    { label: 'Critical open', value: o ? String(o.criticalOpen) : '–', alert: (o?.criticalOpen ?? 0) > 0 },
    { label: 'SLA compliance', value: o?.slaCompliancePercent != null ? `${o.slaCompliancePercent}%` : '–' },
    { label: 'MTTR', value: o ? duration(o.mttrMinutes) : '–' },
  ]
  return (
    <>
      <PageHeader
        title="Command Centre"
        subtitle="Live view of the service desk."
        actions={<Link to="/admin/reports" className="text-sm text-indigo-700 hover:underline">Full reports →</Link>}
      />
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {tiles.map((t) => (
          <div key={t.label} className="rounded-xl bg-white ring-1 ring-slate-200 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">{t.label}</div>
            <div className={`text-2xl font-semibold mt-1 ${!o ? 'text-slate-300' : t.alert ? 'text-red-700' : 'text-slate-900'}`}>{t.value}</div>
          </div>
        ))}
      </div>
      <div className="grid lg:grid-cols-3 gap-6 items-start">
        <div className="lg:col-span-2">
          <LiveFeedPanel />
        </div>
        {user?.role === 'ADMIN' && <TestNotificationCard />}
      </div>
    </>
  )
}
