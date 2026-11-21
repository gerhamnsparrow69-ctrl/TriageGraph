import { PageHeader } from '../../app/PageHeader'
import { useAuth } from '../auth/AuthContext'
import { LiveFeedPanel } from './LiveFeedPanel'
import { TestNotificationCard } from './TestNotificationCard'

const tiles = ['Open incidents', 'Critical open', 'SLA compliance', 'MTTR']

export default function CommandCentre() {
  const { user } = useAuth()
  return (
    <>
      <PageHeader title="Command Centre" subtitle="Live view of the service desk." />
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {tiles.map((t) => (
          <div key={t} className="rounded-xl bg-white ring-1 ring-slate-200 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">{t}</div>
            <div className="text-2xl font-semibold text-slate-300 mt-1">–</div>
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
