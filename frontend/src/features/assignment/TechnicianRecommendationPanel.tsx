import { useQuery } from '@tanstack/react-query'
import { Card, ErrorNote, Meter, Spinner } from '../triage/ui'
import { percent } from '../triage/format'
import { assignmentApi } from './api'

/**
 * Who should take this incident, and why (Intelligent Feature D). Exported for Member 1's incident page;
 * shown to team leads and admins.
 */
export function TechnicianRecommendationPanel({ incidentId, onPick }: { incidentId: number; onPick?: (technicianId: number) => void }) {
  const q = useQuery({ queryKey: ['recommendations', incidentId], queryFn: () => assignmentApi.forIncident(incidentId, 3), retry: false })
  return (
    <Card title="Recommended technicians" subtitle="Ranked by the active assignment strategy">
      {q.isPending && <Spinner />}
      {q.isError && <ErrorNote error={q.error} />}
      {q.data && (
        <ol className="space-y-3">
          {q.data.map((r, i) => (
            <li key={r.technicianId} className="text-sm">
              <div className="flex items-center justify-between gap-2">
                <span className="font-medium">{i + 1}. {r.name}</span>
                <span className="text-slate-500 text-xs">{percent(r.score)}</span>
              </div>
              <Meter value={r.score} tone="bg-teal-500" />
              <div className="text-xs text-slate-500 mt-1">{r.reasons.join(' · ')}</div>
              {onPick && (
                <button type="button" onClick={() => onPick(r.technicianId)} className="text-xs text-teal-700 hover:underline mt-1">
                  Assign to {r.name.split(' ')[0]}
                </button>
              )}
            </li>
          ))}
        </ol>
      )}
    </Card>
  )
}
