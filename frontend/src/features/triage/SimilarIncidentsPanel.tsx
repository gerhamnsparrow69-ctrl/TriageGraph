import { useQuery } from '@tanstack/react-query'
import { triageApi } from './api'
import { Card, ErrorNote, Pill, Spinner } from './ui'
import { percent } from './format'

/** The most similar resolved incidents and how they were fixed. Exported for Member 1's incident page. */
export function SimilarIncidentsPanel({ incidentId }: { incidentId: number }) {
  const q = useQuery({ queryKey: ['similar', incidentId], queryFn: () => triageApi.similar(incidentId, 3), retry: false })
  return (
    <Card title="Similar resolved incidents" subtitle="Found by meaning, not just matching words">
      {q.isPending && <Spinner />}
      {q.isError && <ErrorNote error={q.error} />}
      {q.data && q.data.length === 0 && <p className="text-sm text-slate-500">Nothing similar has been resolved yet.</p>}
      {q.data && q.data.length > 0 && (
        <ul className="divide-y divide-slate-100">
          {q.data.map((s) => (
            <li key={s.code} className="py-2.5">
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm font-medium text-slate-900">{s.code} · {s.title}</span>
                <Pill tone="violet">{percent(s.score)} similar</Pill>
              </div>
              {s.resolutionSummary && <p className="text-sm text-slate-600 mt-1">Fix: {s.resolutionSummary}</p>}
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}
