import { Link } from 'react-router'
import { PriorityBadge, Pill } from '../triage/ui'
import { title as label } from '../triage/format'
import type { QueueItem } from './api'
import { useNow } from './useNow'

function span(minutes: number): string {
  if (minutes < 60) return `${minutes} min`
  if (minutes < 48 * 60) return `${Math.round(minutes / 60)} h`
  return `${Math.round(minutes / 1440)} days`
}

function due(iso: string | null, now: number): string {
  if (!iso) return '—'
  const minutes = Math.round((new Date(iso).getTime() - now) / 60000)
  return minutes < 0 ? `${span(-minutes)} overdue` : `in ${span(minutes)}`
}

/** Open incidents of one technician, most urgent first (order comes from the server). */
export function QueueTable({ items, onReassign }: { items: QueueItem[]; onReassign?: (item: QueueItem) => void }) {
  const now = useNow()
  if (items.length === 0) return <p className="text-sm text-slate-500">Nothing in the queue.</p>
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-xs text-slate-500 border-b border-slate-100">
            <th className="py-2 pr-3">Incident</th><th className="py-2 pr-3">Priority</th><th className="py-2 pr-3">Status</th>
            <th className="py-2 pr-3">Resolution due</th><th className="py-2" />
          </tr>
        </thead>
        <tbody>
          {items.map((i) => {
            const overdue = i.resolutionDueAt !== null && new Date(i.resolutionDueAt).getTime() < now
            return (
              <tr key={i.incidentId} className="border-b border-slate-50">
                <td className="py-2 pr-3"><Link to={`/technician/incidents/${i.incidentId}`} className="font-medium text-teal-700 hover:underline">{i.code}</Link> <span className="text-slate-600">{i.title}</span></td>
                <td className="py-2 pr-3"><PriorityBadge priority={i.priority} /></td>
                <td className="py-2 pr-3"><Pill>{label(i.status)}</Pill></td>
                <td className={`py-2 pr-3 ${overdue ? 'text-red-600 font-medium' : 'text-slate-600'}`}>{due(i.resolutionDueAt, now)}</td>
                <td className="py-2 text-right whitespace-nowrap">
                  <Link to={`/technician/assist/${i.incidentId}`} className="text-teal-700 hover:underline mr-3">Assist</Link>
                  {onReassign && <button type="button" onClick={() => onReassign(i)} className="text-slate-600 hover:underline">Reassign</button>}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
