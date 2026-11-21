import { useEffect, useState, type ReactNode } from 'react'
import { PageHeader } from '../../app/PageHeader'
import { useUsers } from '../dashboard/api'
import { useAuth } from '../auth/AuthContext'
import { humanise } from '../incident/format'
import { downloadCsv, emptyFilters, useIncidentSearch, useSearchOptions, type Filters } from './api'
import { IncidentTable, Pager } from './IncidentTable'

const STATUSES = ['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'PENDING_INFO', 'RESOLVED', 'VERIFIED', 'CLOSED', 'MERGED']
const PRIORITIES = ['P1', 'P2', 'P3', 'P4']
const SLA = ['ON_TRACK', 'AT_RISK', 'BREACHED', 'MET', 'PAUSED']
const SORTS: [string, string][] = [
  ['newest', 'Newest first'],
  ['oldest', 'Oldest first'],
  ['priority', 'Priority'],
  ['due-soonest', 'Due soonest'],
  ['status', 'Status'],
]
const PAGE_SIZE = 20

const input = 'w-full rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500'

/** Text boxes wait until typing pauses, so every key press isn't a request. */
function useDebounced<T>(value: T, ms = 300): T {
  const [v, setV] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms)
    return () => clearTimeout(t)
  }, [value, ms])
  return v
}

function basePathFor(role: string | undefined) {
  if (role === 'EMPLOYEE') return '/employee'
  if (role === 'TECHNICIAN' || role === 'TEAM_LEAD') return '/technician'
  return '/admin'
}

/** Incident search with 11 filters, sort, paging and CSV export. What you can find depends on your role. */
export default function SearchPage() {
  const { user } = useAuth()
  const manager = user?.role === 'ADMIN' || user?.role === 'IT_MANAGER'
  const [filters, setFilters] = useState<Filters>(emptyFilters)
  const [page, setPage] = useState(0)
  const [more, setMore] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [exportError, setExportError] = useState<string | null>(null)

  const q = useDebounced(filters.q)
  const code = useDebounced(filters.code)
  const applied = { ...filters, q, code }
  const result = useIncidentSearch(applied, page, PAGE_SIZE)
  const options = useSearchOptions()

  const set = <K extends keyof Filters>(key: K, value: Filters[K]) => {
    setFilters((f) => ({ ...f, [key]: value }))
    setPage(0)
  }
  const toggle = (key: 'status' | 'priority', value: string) =>
    set(key, filters[key].includes(value) ? filters[key].filter((v) => v !== value) : [...filters[key], value])

  const active = Object.entries(filters).filter(([k, v]) => k !== 'sort' && (Array.isArray(v) ? v.length > 0 : v !== '')).length

  async function exportCsv() {
    setExporting(true)
    setExportError(null)
    try {
      await downloadCsv(applied)
    } catch (e) {
      setExportError(e instanceof Error ? e.message : 'Export failed.')
    } finally {
      setExporting(false)
    }
  }

  return (
    <>
      <PageHeader
        title={manager ? 'Incidents' : 'Search incidents'}
        subtitle={
          manager
            ? 'Every incident in the system. Filter, sort and export.'
            : user?.role === 'TEAM_LEAD'
              ? "Incidents assigned to you and your team."
              : 'Incidents assigned to you.'
        }
        actions={
          <button
            type="button"
            onClick={exportCsv}
            disabled={exporting}
            className="rounded-md bg-white ring-1 ring-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            {exporting ? 'Exporting…' : 'Export CSV'}
          </button>
        }
      />

      <div className="rounded-xl bg-white ring-1 ring-slate-200 p-4 mb-4 space-y-3">
        <div className="grid gap-3 sm:grid-cols-[1fr_10rem_11rem]">
          <input className={input} placeholder="Search title and description…" value={filters.q} onChange={(e) => set('q', e.target.value)} aria-label="Free text" />
          <input className={input} placeholder="INC-1004" value={filters.code} onChange={(e) => set('code', e.target.value)} aria-label="Incident code" />
          <select className={input} value={filters.sort} onChange={(e) => set('sort', e.target.value)} aria-label="Sort">
            {SORTS.map(([v, l]) => (
              <option key={v} value={v}>{l}</option>
            ))}
          </select>
        </div>

        <Chips label="Priority" values={PRIORITIES} selected={filters.priority} onToggle={(v) => toggle('priority', v)} />

        <div className="flex items-center gap-3 text-sm">
          <button type="button" onClick={() => setMore((m) => !m)} className="text-indigo-700 hover:underline">
            {more ? 'Fewer filters' : 'More filters'}
          </button>
          {active > 0 && (
            <button type="button" onClick={() => { setFilters({ ...emptyFilters, sort: filters.sort }); setPage(0) }} className="text-slate-500 hover:underline">
              Clear {active} filter{active === 1 ? '' : 's'}
            </button>
          )}
        </div>

        {more && (
          <div className="space-y-3 pt-1">
            <Chips label="Status" values={STATUSES} selected={filters.status} onToggle={(v) => toggle('status', v)} />
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <Field label="Reported from">
                <input type="date" className={input} value={filters.from} onChange={(e) => set('from', e.target.value)} />
              </Field>
              <Field label="Reported to">
                <input type="date" className={input} value={filters.to} onChange={(e) => set('to', e.target.value)} />
              </Field>
              <Field label="Category">
                <select className={input} value={filters.category} onChange={(e) => set('category', e.target.value)}>
                  <option value="">Any</option>
                  {options.data?.categories.map((c) => <option key={c} value={c}>{humanise(c)}</option>)}
                </select>
              </Field>
              <Field label="Service">
                <select className={input} value={filters.serviceId} onChange={(e) => set('serviceId', e.target.value)}>
                  <option value="">Any</option>
                  {options.data?.services.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
              </Field>
              <Field label="SLA status">
                <select className={input} value={filters.slaStatus} onChange={(e) => set('slaStatus', e.target.value)}>
                  <option value="">Any</option>
                  {SLA.map((s) => <option key={s} value={s}>{humanise(s)}</option>)}
                </select>
              </Field>
              {user?.role !== 'TECHNICIAN' && (
                <Field label="Technician">
                  <select className={input} value={filters.technicianId} onChange={(e) => set('technicianId', e.target.value)}>
                    <option value="">Anyone</option>
                    {options.data?.technicians.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                  </select>
                </Field>
              )}
              {manager && <ReporterField value={filters.reporterId} onChange={(v) => set('reporterId', v)} />}
              <Field label="Major incident">
                <select className={input} value={filters.major} onChange={(e) => set('major', e.target.value)}>
                  <option value="">Either</option>
                  <option value="true">Part of a major incident</option>
                  <option value="false">Not part of one</option>
                </select>
              </Field>
            </div>
          </div>
        )}
      </div>

      {exportError && <p className="mb-3 text-sm text-red-700">{exportError}</p>}
      {result.isError && <p className="mb-3 text-sm text-red-700">{result.error.message}</p>}
      {result.isPending && <p className="text-sm text-slate-500">Loading…</p>}
      {result.data && result.data.total === 0 && (
        <div className="rounded-xl bg-white ring-1 ring-slate-200 p-8 text-center text-sm text-slate-500">No incidents match these filters.</div>
      )}
      {result.data && result.data.total > 0 && (
        <div className={result.isFetching ? 'opacity-70 transition-opacity' : ''}>
          <p className="text-xs text-slate-500 mb-2">{result.data.total} incident{result.data.total === 1 ? '' : 's'}</p>
          <IncidentTable rows={result.data.items} basePath={basePathFor(user?.role)} />
          <Pager page={page} size={PAGE_SIZE} total={result.data.total} onPage={setPage} />
        </div>
      )}
    </>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="block text-xs font-medium text-slate-600 mb-1">{label}</span>
      {children}
    </label>
  )
}

function Chips({ label, values, selected, onToggle }: { label: string; values: string[]; selected: string[]; onToggle: (v: string) => void }) {
  return (
    <div className="flex flex-wrap items-center gap-1.5">
      <span className="text-xs font-medium text-slate-600 mr-1">{label}</span>
      {values.map((v) => {
        const on = selected.includes(v)
        return (
          <button
            key={v}
            type="button"
            aria-pressed={on}
            onClick={() => onToggle(v)}
            className={`rounded-full px-2.5 py-0.5 text-xs ring-1 ${on ? 'bg-indigo-600 text-white ring-indigo-600' : 'bg-white text-slate-600 ring-slate-300 hover:bg-slate-50'}`}
          >
            {v.startsWith('P') && v.length === 2 ? v : humanise(v)}
          </button>
        )
      })}
    </div>
  )
}

/** Admins and the IT manager can filter by who reported it. */
function ReporterField({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  const users = useUsers()
  return (
    <Field label="Reported by">
      <select className={input} value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">Anyone</option>
        {users.data?.map((u) => <option key={u.id} value={String(u.id)}>{u.name}</option>)}
      </select>
    </Field>
  )
}
