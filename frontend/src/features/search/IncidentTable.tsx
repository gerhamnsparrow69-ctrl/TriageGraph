import { Link } from 'react-router'
import { humanise, priorityClass, statusClass } from '../incident/format'
import type { IncidentRow, SlaStatus } from './api'

const slaClass: Record<SlaStatus, string> = {
  ON_TRACK: 'text-emerald-700',
  AT_RISK: 'text-amber-700 font-medium',
  BREACHED: 'text-red-700 font-semibold',
  MET: 'text-slate-500',
  PAUSED: 'text-slate-500 italic',
}

const DATE = new Intl.DateTimeFormat('en-IN', { timeZone: 'Asia/Kolkata', day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false })

/** Search results. basePath is the layout prefix, so a row opens /admin/incidents/7 or /employee/incidents/7. */
export function IncidentTable({ rows, basePath, showPeople = true }: { rows: IncidentRow[]; basePath: string; showPeople?: boolean }) {
  return (
    <div className="relative rounded-xl bg-white ring-1 ring-slate-200 overflow-x-auto">
      <table className="w-full text-sm">
        <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
          <tr>
            <th className="px-4 py-2.5">Incident</th>
            <th className="px-3 py-2.5">Priority</th>
            <th className="px-3 py-2.5">Status</th>
            <th className="px-3 py-2.5 hidden md:table-cell">Service</th>
            {showPeople && <th className="px-3 py-2.5 hidden lg:table-cell">Assignee</th>}
            <th className="px-3 py-2.5">SLA</th>
            <th className="px-3 py-2.5 hidden sm:table-cell">Reported</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id} className="border-t border-slate-100 hover:bg-slate-50 align-top">
              <td className="px-4 py-2.5 min-w-48">
                <Link to={`${basePath}/incidents/${r.id}`} className="font-medium text-slate-900 hover:text-indigo-700">
                  {r.title}
                </Link>
                <div className="text-xs text-slate-500 mt-0.5">
                  <span className="font-mono">{r.code}</span>
                  {r.categoryCode && <> · {humanise(r.categoryCode)}</>}
                  {r.major && <span className="ml-1 rounded bg-red-100 text-red-700 px-1">major</span>}
                </div>
              </td>
              <td className="px-3 py-2.5">
                {r.priority ? (
                  <span className={`rounded px-1.5 py-0.5 text-xs font-semibold ${priorityClass[r.priority] ?? ''}`}>{r.priority}</span>
                ) : (
                  <span className="text-slate-400">–</span>
                )}
              </td>
              <td className="px-3 py-2.5 whitespace-nowrap">
                <span className={`rounded-full px-2 py-0.5 text-xs ${statusClass[r.status] ?? 'bg-slate-100'}`}>{humanise(r.status)}</span>
              </td>
              <td className="px-3 py-2.5 hidden md:table-cell text-slate-600">{r.serviceName ?? '–'}</td>
              {showPeople && <td className="px-3 py-2.5 hidden lg:table-cell text-slate-600">{r.assigneeName ?? 'Unassigned'}</td>}
              <td className={`px-3 py-2.5 whitespace-nowrap text-xs ${slaClass[r.slaStatus]}`}>{humanise(r.slaStatus)}</td>
              <td className="px-3 py-2.5 hidden sm:table-cell whitespace-nowrap text-xs text-slate-500">{DATE.format(new Date(r.createdAt))}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function Pager({ page, size, total, onPage }: { page: number; size: number; total: number; onPage: (p: number) => void }) {
  const last = Math.max(0, Math.ceil(total / size) - 1)
  if (total <= size) return null
  const btn = 'rounded-md ring-1 ring-slate-300 bg-white px-3 py-1 text-sm disabled:opacity-40'
  return (
    <div className="flex items-center justify-between mt-3 text-sm text-slate-600">
      <span>
        {page * size + 1}–{Math.min(total, (page + 1) * size)} of {total}
      </span>
      <div className="flex gap-2">
        <button type="button" className={btn} disabled={page === 0} onClick={() => onPage(page - 1)}>
          Previous
        </button>
        <button type="button" className={btn} disabled={page >= last} onClick={() => onPage(page + 1)}>
          Next
        </button>
      </div>
    </div>
  )
}
