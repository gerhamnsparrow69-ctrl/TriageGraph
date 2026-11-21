import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { PageHeader } from '../../app/PageHeader'
import { Card, ErrorNote, Pill, Spinner } from '../triage/ui'
import { assignmentApi } from './api'
import { QueueTable } from './QueueTable'
import { WorkloadGauge } from './WorkloadGauge'

/** The technician's own workload and queue (refreshes every 30 s). */
export default function WorkloadPage() {
  const q = useQuery({ queryKey: ['workload', 'me'], queryFn: assignmentApi.myWorkload, refetchInterval: 30_000 })
  return (
    <>
      <PageHeader title="My workload" subtitle="Your open incidents, most urgent first."
        actions={<Link to="/technician/knowledge" className="text-sm text-teal-700 hover:underline">Search the knowledge base →</Link>} />
      {q.isPending && <Spinner />}
      {q.isError && <ErrorNote error={q.error} />}
      {q.data && (
        <div className="grid lg:grid-cols-[18rem_1fr] gap-5">
          <Card title={q.data.name}>
            <WorkloadGauge w={q.data} />
            <div className="flex flex-wrap gap-1 mt-3">{q.data.skills.map((s) => <Pill key={s} tone="teal">{s}</Pill>)}</div>
          </Card>
          <Card title="Queue">
            {!q.data.incidentDataAvailable && (
              <p className="text-xs text-slate-500 mb-2">Your incidents appear here once the incident module is connected.</p>
            )}
            <QueueTable items={q.data.queue} />
          </Card>
        </div>
      )}
    </>
  )
}
