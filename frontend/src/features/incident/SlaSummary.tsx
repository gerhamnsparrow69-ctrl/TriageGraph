import { formatIst, relative } from './format'
import type { IncidentView } from './types'
import { useNow } from './useNow'

type Tone = 'ok' | 'warn' | 'bad' | 'done' | 'paused'
const toneClass: Record<Tone, string> = {
  ok: 'bg-emerald-50 text-emerald-800 ring-emerald-200',
  warn: 'bg-amber-50 text-amber-800 ring-amber-200',
  bad: 'bg-red-50 text-red-800 ring-red-200',
  done: 'bg-slate-50 text-slate-600 ring-slate-200',
  paused: 'bg-sky-50 text-sky-800 ring-sky-200',
}

function Chip({ label, tone, detail, title }: { label: string; tone: Tone; detail: string; title?: string }) {
  return (
    <span title={title} className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs ring-1 ${toneClass[tone]}`}>
      <span className="font-semibold">{label}</span>
      {detail}
    </span>
  )
}

/** Due-time chips. M3's live SLA chips (warning/breach from SlaMonitor) can replace this via the side-panel slot. */
export function SlaSummary({ incident }: { incident: IncidentView }) {
  const now = useNow()
  const { responseDueAt, resolutionDueAt, respondedAt, paused } = incident.sla
  const finished = ['RESOLVED', 'VERIFIED', 'CLOSED', 'MERGED'].includes(incident.status)
  const toneFor = (due: string): Tone => {
    const left = new Date(due).getTime() - now
    return left < 0 ? 'bad' : left < 15 * 60_000 ? 'warn' : 'ok'
  }
  return (
    <div className="flex flex-wrap gap-2" aria-label="SLA">
      {responseDueAt && (respondedAt
        ? <Chip label="Response" tone={new Date(respondedAt) <= new Date(responseDueAt) ? 'done' : 'bad'}
            detail={new Date(respondedAt) <= new Date(responseDueAt) ? 'met' : 'missed'} title={`Acknowledged ${formatIst(respondedAt)}`} />
        : <Chip label="Response" tone={toneFor(responseDueAt)} detail={`due ${relative(responseDueAt, now)}`} title={formatIst(responseDueAt)} />)}
      {resolutionDueAt && (finished
        ? <Chip label="Resolution" tone="done" detail="stopped" title={formatIst(resolutionDueAt)} />
        : paused
          ? <Chip label="Resolution" tone="paused" detail="paused (waiting on reporter)" />
          : <Chip label="Resolution" tone={toneFor(resolutionDueAt)} detail={`due ${relative(resolutionDueAt, now)}`} title={formatIst(resolutionDueAt)} />)}
    </div>
  )
}
