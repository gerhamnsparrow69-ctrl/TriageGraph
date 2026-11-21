import type { ReactNode } from 'react'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { PageHeader } from '../../app/PageHeader'
import { humanise } from '../incident/format'
import { duration, useReports, type Dashboard, type ServiceAnalysis } from './api'

/** One colour per category, in a fixed order so a category keeps its colour between visits. */
const CATEGORY_COLOURS = ['#4f46e5', '#0891b2', '#059669', '#d97706', '#dc2626', '#7c3aed', '#db2777', '#65a30d', '#0f766e', '#64748b']

const HEALTH: Record<ServiceAnalysis['health'], string> = {
  OUTAGE: 'bg-red-100 text-red-800',
  DEGRADED: 'bg-amber-100 text-amber-800',
  HEALTHY: 'bg-emerald-100 text-emerald-800',
}

const SHORT_DATE = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', timeZone: 'Asia/Kolkata' })
const TIME = new Intl.DateTimeFormat('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: 'Asia/Kolkata' })

/** The six reports of the blueprint: overview, trend, SLA by priority, technician workload, services, AI. */
export default function ReportsPage() {
  const q = useReports()
  return (
    <>
      <PageHeader
        title="Reports"
        subtitle={q.data ? `Live figures, updated ${TIME.format(new Date(q.data.generatedAt))} IST.` : 'Service desk analytics.'}
        actions={
          <button type="button" onClick={() => q.refetch()} disabled={q.isFetching}
            className="rounded-md bg-white ring-1 ring-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50 disabled:opacity-50">
            {q.isFetching ? 'Refreshing…' : 'Refresh'}
          </button>
        }
      />
      {q.isPending && <p className="text-sm text-slate-500">Loading reports…</p>}
      {q.isError && <p className="text-sm text-red-700">{q.error.message}</p>}
      {q.data && <Reports data={q.data} />}
    </>
  )
}

function Reports({ data }: { data: Dashboard }) {
  const o = data.overview
  const trend = data.trend.days.map((d) => ({ day: SHORT_DATE.format(new Date(`${d.date}T12:00:00+05:30`)), ...d.counts }))
  const services = data.services.filter((s) => s.incidents > 0)
  return (
    <div className="space-y-6">
      <section aria-label="Incident overview" className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
        <Tile label="Total incidents" value={o.total} />
        <Tile label="Open" value={o.open} />
        <Tile label="Critical open (P1)" value={o.criticalOpen} tone={o.criticalOpen > 0 ? 'red' : undefined} />
        <Tile label="Resolved" value={o.resolved} hint="awaiting verification" />
        <Tile label="Closed" value={o.closed} />
        <Tile label="SLA violations" value={o.slaViolations} tone={o.slaViolations > 0 ? 'amber' : undefined} />
        <Tile label="SLA compliance" value={o.slaCompliancePercent === null ? '–' : `${o.slaCompliancePercent}%`} />
        <Tile label="MTTA" value={duration(o.mttaMinutes)} hint="mean time to acknowledge" />
        <Tile label="MTTR" value={duration(o.mttrMinutes)} hint="mean time to resolve" />
        <Tile label="Tickets avoided" value={data.ai.ticketsAvoided} hint="joined or merged duplicates" tone="green" />
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card title="Incident trend" subtitle="Reported per day by category, last 14 days">
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={trend} margin={{ top: 8, right: 8, bottom: 0, left: -24 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="day" tick={{ fontSize: 11 }} interval="preserveStartEnd" />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Legend wrapperStyle={{ fontSize: 11 }} formatter={(v) => humanise(String(v))} />
                {data.trend.categories.map((c, i) => (
                  <Bar key={c} dataKey={c} stackId="day" fill={CATEGORY_COLOURS[i % CATEGORY_COLOURS.length]} name={c} />
                ))}
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card title="SLA compliance by priority" subtitle="Resolution target met vs breached; open ones still on track">
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data.slaByPriority} margin={{ top: 8, right: 8, bottom: 0, left: -24 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="priority" tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Bar dataKey="met" stackId="sla" fill="#059669" name="Met" />
                <Bar dataKey="breached" stackId="sla" fill="#dc2626" name="Breached" />
                <Bar dataKey="openOnTrack" stackId="sla" fill="#a5b4fc" name="Open, on track" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <Card title="Technician workload" subtitle="Active incidents against each person's maximum load">
        <div className="h-56 mb-4">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data.technicians} margin={{ top: 8, right: 8, bottom: 0, left: -24 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
              <XAxis dataKey="name" tick={{ fontSize: 11 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
              <Tooltip />
              <Legend wrapperStyle={{ fontSize: 11 }} />
              <Bar dataKey="active" fill="#4f46e5" name="Active" />
              <Bar dataKey="maxLoad" fill="#e0e7ff" name="Max load" />
            </BarChart>
          </ResponsiveContainer>
        </div>
        <Table head={['Technician', 'Active / max', 'Resolved (7 days)', 'Avg resolution', 'Available']}>
          {data.technicians.map((t) => (
            <tr key={t.id} className="border-t border-slate-100">
              <td className="py-2 pr-3">
                {t.name} <span className="text-xs text-slate-400">{t.role === 'TEAM_LEAD' ? 'team lead' : ''}</span>
              </td>
              <td className={`pr-3 ${t.active >= t.maxLoad && t.maxLoad > 0 ? 'text-red-700 font-medium' : ''}`}>{t.active} / {t.maxLoad}</td>
              <td className="pr-3">{t.resolvedLast7Days}</td>
              <td className="pr-3">{duration(t.avgResolutionMinutes)}</td>
              <td>{t.available ? 'Yes' : <span className="text-slate-400">No</span>}</td>
            </tr>
          ))}
        </Table>
      </Card>

      <Card title="Service analysis" subtitle="Incidents per service, current health and the most common root causes">
        {services.length > 0 && (
          <div className="mb-4" style={{ height: Math.max(160, services.length * 28) }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={services} layout="vertical" margin={{ top: 0, right: 8, bottom: 0, left: 40 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" horizontal={false} />
                <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11 }} />
                <YAxis type="category" dataKey="name" tick={{ fontSize: 11 }} width={110} />
                <Tooltip />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Bar dataKey="incidents" fill="#94a3b8" name="All incidents" />
                <Bar dataKey="open" fill="#d97706" name="Open" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
        <Table head={['Service', 'Criticality', 'Incidents', 'Open', 'Health', 'Top root causes']}>
          {data.services.map((s) => (
            <tr key={s.id} className="border-t border-slate-100 align-top">
              <td className="py-2 pr-3 font-medium text-slate-800">{s.name}</td>
              <td className="pr-3 text-xs text-slate-500">{humanise(s.criticality)}</td>
              <td className="pr-3">{s.incidents}</td>
              <td className="pr-3">{s.open}</td>
              <td className="pr-3"><span className={`rounded-full px-2 py-0.5 text-xs ${HEALTH[s.health]}`}>{humanise(s.health)}</span></td>
              <td className="text-xs text-slate-600">{s.topRootCauses.join(' · ') || '–'}</td>
            </tr>
          ))}
        </Table>
      </Card>

      <Card title="AI effectiveness" subtitle="How much the intelligent features saved">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <Tile label="Suggestion accepted" value={data.ai.suggestionAcceptancePercent === null ? '–' : `${data.ai.suggestionAcceptancePercent}%`}
            hint={`of ${data.ai.classified} classified`} />
          <Tile label="Joined existing" value={data.ai.duplicatesJoined} hint="people who joined instead of filing" />
          <Tile label="Duplicates merged" value={data.ai.duplicatesMerged} />
          <Tile label="Major incidents found" value={data.ai.majorIncidents} />
        </div>
      </Card>
    </div>
  )
}

function Tile({ label, value, hint, tone }: { label: string; value: ReactNode; hint?: string; tone?: 'red' | 'amber' | 'green' }) {
  const colour = tone === 'red' ? 'text-red-700' : tone === 'amber' ? 'text-amber-700' : tone === 'green' ? 'text-emerald-700' : 'text-slate-900'
  return (
    <div className="rounded-xl bg-white ring-1 ring-slate-200 p-4">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className={`text-2xl font-semibold mt-1 ${colour}`}>{value}</div>
      {hint && <div className="text-xs text-slate-400 mt-0.5">{hint}</div>}
    </div>
  )
}

function Card({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <section className="rounded-xl bg-white ring-1 ring-slate-200 p-5">
      <h2 className="font-semibold text-slate-900">{title}</h2>
      {subtitle && <p className="text-xs text-slate-500 mb-3">{subtitle}</p>}
      {children}
    </section>
  )
}

function Table({ head, children }: { head: string[]; children: ReactNode }) {
  return (
    <div className="relative overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-xs uppercase tracking-wide text-slate-500">
            {head.map((h) => <th key={h} className="pb-2 pr-3 font-medium whitespace-nowrap">{h}</th>)}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  )
}
