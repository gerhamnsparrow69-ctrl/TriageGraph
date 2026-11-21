import { PageHeader } from '../../app/PageHeader'
import { useMarkAllRead, useMarkRead, useNotifications, type NotificationItem } from './api'

const priorityClass: Record<string, string> = {
  P1: 'bg-red-100 text-red-700',
  P2: 'bg-orange-100 text-orange-700',
  P3: 'bg-yellow-100 text-yellow-800',
  P4: 'bg-slate-100 text-slate-600',
}

function timeAgo(iso: string) {
  const s = Math.round((Date.now() - new Date(iso).getTime()) / 1000)
  if (s < 60) return 'just now'
  if (s < 3600) return `${Math.floor(s / 60)} min ago`
  if (s < 86400) return `${Math.floor(s / 3600)} h ago`
  return new Date(iso).toLocaleDateString()
}

function Row({ n }: { n: NotificationItem }) {
  const markRead = useMarkRead()
  return (
    <li className={`flex gap-3 p-4 ${n.read ? 'bg-white' : 'bg-blue-50/60'}`}>
      <span className={`mt-1.5 w-2 h-2 rounded-full shrink-0 ${n.read ? 'bg-transparent' : 'bg-blue-500'}`} />
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          {n.priority && (
            <span className={`text-[11px] font-semibold px-1.5 py-0.5 rounded ${priorityClass[n.priority]}`}>{n.priority}</span>
          )}
          <span className="font-medium text-slate-900">{n.subject.replace(/^\[P\d\] /, '')}</span>
        </div>
        <p className="text-sm text-slate-600 mt-1">{n.body}</p>
        <div className="text-xs text-slate-400 mt-1">{timeAgo(n.createdAt)}</div>
      </div>
      {!n.read && (
        <button onClick={() => markRead.mutate(n.id)} disabled={markRead.isPending}
          className="self-start text-xs text-blue-600 hover:underline whitespace-nowrap">
          Mark read
        </button>
      )}
    </li>
  )
}

export default function NotificationsPage() {
  const { data, isPending, isError } = useNotifications()
  const markAll = useMarkAllRead()
  const unread = data?.filter((n) => !n.read).length ?? 0

  return (
    <>
      <PageHeader
        title="Notifications"
        subtitle={unread ? `${unread} unread` : 'You are all caught up.'}
        actions={
          unread > 0 && (
            <button onClick={() => markAll.mutate()} disabled={markAll.isPending}
              className="rounded-md bg-white ring-1 ring-slate-300 px-3 py-1.5 text-sm hover:bg-slate-50">
              Mark all as read
            </button>
          )
        }
      />
      {isPending && <p className="text-slate-500 text-sm">Loading…</p>}
      {isError && <p className="text-red-600 text-sm">Could not load notifications.</p>}
      {data && data.length === 0 && (
        <div className="rounded-xl bg-white ring-1 ring-slate-200 p-8 text-center text-slate-500 text-sm">
          No notifications yet.
        </div>
      )}
      {data && data.length > 0 && (
        <ul className="rounded-xl ring-1 ring-slate-200 divide-y divide-slate-200 overflow-hidden">
          {data.map((n) => <Row key={n.id} n={n} />)}
        </ul>
      )}
    </>
  )
}
