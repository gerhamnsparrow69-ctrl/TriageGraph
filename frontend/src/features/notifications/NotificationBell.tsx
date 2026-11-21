import { Link } from 'react-router'
import { useUnreadCount } from './api'

export function NotificationBell({ to }: { to: string }) {
  const { data: unread = 0 } = useUnreadCount()
  const label = unread > 0 ? `${unread} unread notifications` : 'Notifications'
  return (
    <Link to={to} aria-label={label} title={label}
      className="relative inline-flex items-center justify-center w-9 h-9 rounded-md hover:bg-white/10">
      <svg viewBox="0 0 24 24" className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
        <path d="M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
      {unread > 0 && (
        <span className="absolute -top-0.5 -right-0.5 min-w-[1.1rem] h-[1.1rem] px-1 rounded-full bg-red-500 text-white text-[10px] font-semibold grid place-items-center">
          {unread > 99 ? '99+' : unread}
        </span>
      )}
    </Link>
  )
}
