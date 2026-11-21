import { formatIst, humanise } from '../incident/format'
import { useTimeline } from './api'
import { actorLabel, shortHash, transitionLabel } from './describe'

/** Every recorded step of one incident, oldest first, straight from the hash-chained audit trail. */
export function IncidentTimeline({ incidentId }: { incidentId: number }) {
  const { data, isPending, isError } = useTimeline(incidentId)

  return (
    <section aria-label="Timeline" className="rounded-xl bg-white ring-1 ring-slate-200 p-4">
      <h2 className="font-semibold text-slate-900 mb-3">Timeline</h2>
      {isPending && <p className="text-sm text-slate-500">Loading…</p>}
      {isError && <p className="text-sm text-red-600">Could not load the timeline.</p>}
      {data && data.length === 0 && <p className="text-sm text-slate-500">Nothing recorded yet.</p>}
      {data && data.length > 0 && (
        <ol className="relative border-l border-slate-200 ml-2 space-y-4">
          {data.map((e) => {
            const transition = transitionLabel(e)
            return (
              <li key={e.id} className="ml-4">
                <span className="absolute -left-1.5 mt-1.5 h-3 w-3 rounded-full bg-slate-400 ring-2 ring-white" />
                <div className="flex flex-wrap items-baseline gap-x-2 text-sm">
                  <span className="font-medium text-slate-900">{humanise(e.action)}</span>
                  {transition && <span className="text-xs text-slate-500">{transition}</span>}
                  <span className="text-xs text-slate-400">{actorLabel(e)}</span>
                </div>
                {e.description && <p className="text-sm text-slate-600">{e.description}</p>}
                <div className="flex gap-3 text-xs text-slate-400">
                  <time dateTime={e.occurredAt}>{formatIst(e.occurredAt)}</time>
                  <span className="font-mono" title={e.hash}>#{shortHash(e.hash)}</span>
                </div>
              </li>
            )
          })}
        </ol>
      )}
    </section>
  )
}
