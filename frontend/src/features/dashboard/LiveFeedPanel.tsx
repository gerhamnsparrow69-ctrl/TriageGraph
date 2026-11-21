import { useLiveStatus } from '../../app/live/LiveContext'
import { useLiveFeed, type FeedItem } from './api'

const tone: Record<string, string> = {
  REPORTED: 'bg-sky-100 text-sky-800',
  ASSIGNED: 'bg-indigo-100 text-indigo-800',
  PRIORITY: 'bg-amber-100 text-amber-800',
  STATUS: 'bg-slate-100 text-slate-700',
  SLA_WARNING: 'bg-orange-100 text-orange-800',
  SLA_BREACHED: 'bg-red-100 text-red-800',
  ESCALATED: 'bg-red-100 text-red-800',
  MAJOR: 'bg-red-600 text-white',
  NOTE: 'bg-slate-100 text-slate-700',
  DUPLICATE: 'bg-violet-100 text-violet-800',
  FEEDBACK: 'bg-emerald-100 text-emerald-800',
  CATALOG: 'bg-teal-100 text-teal-800',
  TEST: 'bg-slate-200 text-slate-700',
}

const label = (type: string) => type.replace('_', ' ').toLowerCase()

function Row({ item }: { item: FeedItem }) {
  return (
    <li className="flex items-start gap-3 px-4 py-2.5">
      <time className="text-xs tabular-nums text-slate-400 pt-0.5 w-[5.5rem] shrink-0 whitespace-nowrap" dateTime={item.at}>
        {new Date(item.at).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false, timeZone: 'Asia/Kolkata' })}
      </time>
      <span className={`text-[11px] font-medium uppercase tracking-wide rounded px-1.5 py-0.5 shrink-0 ${tone[item.type] ?? tone.STATUS}`}>
        {label(item.type)}
      </span>
      <span className="text-sm text-slate-700 min-w-0">
        {item.incidentCode && <span className="font-mono font-medium text-slate-900 mr-1.5">{item.incidentCode}</span>}
        {item.summary}
      </span>
    </li>
  )
}

export function LiveFeedPanel() {
  const { data = [], isLoading, isError } = useLiveFeed()
  const live = useLiveStatus()
  return (
    <section className="rounded-xl bg-white ring-1 ring-slate-200 overflow-hidden">
      <header className="flex items-center justify-between px-4 py-3 border-b border-slate-100">
        <h2 className="font-semibold text-slate-900">Live event feed</h2>
        <span className="flex items-center gap-1.5 text-xs text-slate-500" role="status">
          <span className={`w-2 h-2 rounded-full ${live ? 'bg-emerald-500 animate-pulse' : 'bg-slate-300'}`} />
          {live ? 'Live' : 'Reconnecting…'}
        </span>
      </header>
      {isLoading ? (
        <p className="p-6 text-sm text-slate-500">Loading…</p>
      ) : isError ? (
        <p className="p-6 text-sm text-red-600">Couldn't load the feed.</p>
      ) : data.length === 0 ? (
        <p className="p-6 text-sm text-slate-500">Nothing yet. New incidents, assignments and SLA alerts appear here as they happen.</p>
      ) : (
        <ol className="divide-y divide-slate-100 max-h-[28rem] overflow-y-auto" aria-live="polite">
          {data.map((item, i) => <Row key={`${item.at}-${i}`} item={item} />)}
        </ol>
      )}
    </section>
  )
}
