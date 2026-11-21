import type { ReactNode } from 'react'

/** Small presentational pieces shared by Member 2's pages. */

export function Card({ title, subtitle, actions, children, className = '' }: {
  title?: string
  subtitle?: string
  actions?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <section className={`rounded-xl bg-white ring-1 ring-slate-200 p-5 ${className}`}>
      {(title || actions) && (
        <div className="flex flex-wrap items-start justify-between gap-2 mb-3">
          <div>
            {title && <h2 className="text-base font-semibold text-slate-900">{title}</h2>}
            {subtitle && <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>}
          </div>
          {actions}
        </div>
      )}
      {children}
    </section>
  )
}

export function Pill({ children, tone = 'slate' }: { children: ReactNode; tone?: 'slate' | 'blue' | 'green' | 'amber' | 'red' | 'violet' | 'teal' }) {
  const tones = {
    slate: 'bg-slate-100 text-slate-700',
    blue: 'bg-blue-100 text-blue-800',
    green: 'bg-emerald-100 text-emerald-800',
    amber: 'bg-amber-100 text-amber-800',
    red: 'bg-red-100 text-red-800',
    violet: 'bg-violet-100 text-violet-800',
    teal: 'bg-teal-100 text-teal-800',
  }
  return <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${tones[tone]}`}>{children}</span>
}

export function PriorityBadge({ priority }: { priority: string | null | undefined }) {
  const tone = priority === 'P1' ? 'red' : priority === 'P2' ? 'amber' : priority === 'P3' ? 'blue' : 'slate'
  return <Pill tone={tone}>{priority ?? 'P?'}</Pill>
}

/** A thin horizontal bar for a 0–1 value. */
export function Meter({ value, tone = 'bg-blue-500' }: { value: number; tone?: string }) {
  const pct = Math.round(Math.max(0, Math.min(1, value)) * 100)
  return (
    <div className="h-1.5 w-full rounded-full bg-slate-100" role="meter" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
      <div className={`h-1.5 rounded-full ${tone}`} style={{ width: `${pct}%` }} />
    </div>
  )
}

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return <div className="text-sm text-slate-500 py-4" role="status">{label}</div>
}

export function ErrorNote({ error }: { error: unknown }) {
  const message = error instanceof Error ? error.message : 'Something went wrong.'
  return <div className="rounded-md bg-red-50 text-red-700 text-sm px-3 py-2" role="alert">{message}</div>
}
