import { useParams } from 'react-router'
import { ApiError } from '../../api/client'
import { useAuth } from '../auth/AuthContext'
import { IncidentTimeline } from '../audit/IncidentTimeline'
import { ActionBar } from './ActionBar'
import { useIncident } from './api'
import { formatIst, humanise, priorityClass, statusClass } from './format'
import { NotesPanel } from './NotesPanel'
import { SlaSummary } from './SlaSummary'
import { incidentSidePanels } from './slots'

/** One incident, shared by every role. Buttons come from allowedActions; the page itself has no lifecycle rules. */
export default function IncidentDetailPage() {
  const id = Number(useParams().id)
  const { user } = useAuth()
  const { data: incident, isPending, error } = useIncident(id)
  const isStaff = !!user && user.role !== 'EMPLOYEE'

  if (!(id > 0)) return <p className="text-red-600 text-sm">That is not a valid incident link.</p>
  if (isPending) return <p className="text-slate-500 text-sm">Loading incident…</p>
  if (error || !incident) {
    const status = error instanceof ApiError ? error.problem.status : 0
    return (
      <div className="rounded-xl bg-white ring-1 ring-slate-200 p-8 text-center text-sm">
        <p className="font-medium text-slate-900">
          {status === 403 ? 'You do not have access to this incident.' : status === 404 ? 'Incident not found.' : 'Could not load the incident.'}
        </p>
      </div>
    )
  }

  return (
    <article className="space-y-4">
      <header className="rounded-xl bg-white ring-1 ring-slate-200 p-5">
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <span className="font-mono text-slate-500">{incident.code}</span>
          <span data-testid="status" className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${statusClass[incident.status]}`}>
            {humanise(incident.status)}
          </span>
          {incident.priority && (
            <span title={incident.priorityRationale ?? undefined}
              className={`rounded px-2 py-0.5 text-xs font-bold ${priorityClass[incident.priority]}`}>
              {incident.priority}{incident.priorityScore != null && ` · ${incident.priorityScore.toFixed(1)}`}
            </span>
          )}
          {incident.categoryCode && <span className="text-xs text-slate-500">{humanise(incident.categoryCode)}</span>}
        </div>
        <h1 className="mt-2 text-2xl font-semibold text-slate-900">{incident.title}</h1>
        <p className="mt-2 whitespace-pre-wrap text-slate-700">{incident.description}</p>
        <dl className="mt-4 grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
          <div><dt className="text-slate-500">Reported</dt>
            <dd>{formatIst(incident.createdAt)}
              <span className="block text-xs text-slate-500">by {incident.reporterName ?? 'the reporter'}</span></dd></div>
          <div><dt className="text-slate-500">Impact / urgency</dt><dd>{humanise(incident.impact)} / {humanise(incident.urgency)}</dd></div>
          <div><dt className="text-slate-500">Affected users</dt><dd>{incident.affectedUsers}</dd></div>
          <div><dt className="text-slate-500">Assignee</dt>
            <dd>{incident.assigneeId == null ? 'Not assigned yet' : (incident.assigneeName ?? 'Assigned technician')}</dd></div>
        </dl>
        <div className="mt-4"><SlaSummary incident={incident} /></div>
        {incident.awaitingAssignmentReason && (
          <p className="mt-3 rounded-md bg-amber-50 ring-1 ring-amber-200 p-2 text-sm text-amber-800">
            Waiting for a team lead to assign: {incident.awaitingAssignmentReason}
          </p>
        )}
        {incident.priorityRationale && isStaff && (
          <p className="mt-3 text-xs text-slate-500">Priority: {incident.priorityRationale}</p>
        )}
      </header>

      <ActionBar incident={incident} />

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          {incident.resolution && (
            <section aria-label="Resolution" className="rounded-xl bg-emerald-50 ring-1 ring-emerald-200 p-4">
              <h2 className="font-semibold text-emerald-900">Resolution</h2>
              <p className="mt-1 whitespace-pre-wrap text-sm text-emerald-900">{incident.resolution.summary}</p>
              {incident.resolution.rootCause && (
                <p className="mt-1 text-sm text-emerald-800">Root cause: {incident.resolution.rootCause}</p>
              )}
              <p className="mt-1 text-xs text-emerald-700">Resolved {formatIst(incident.resolution.resolvedAt)}</p>
              {incident.allowedActions.includes('VERIFY') && (
                <p className="mt-2 text-sm text-emerald-900">Is everything working again? Confirm or reopen it above.</p>
              )}
            </section>
          )}
          {incident.feedbackRating != null && (
            <p className="text-sm text-slate-600">Reporter rated this fix {'★'.repeat(incident.feedbackRating)}</p>
          )}
          <NotesPanel incident={incident} isStaff={isStaff} />
          <IncidentTimeline incidentId={incident.id} />
        </div>
        <aside className="space-y-4">
          {incidentSidePanels.map(({ key, Component }) => <Component key={key} incident={incident} />)}
        </aside>
      </div>
    </article>
  )
}
