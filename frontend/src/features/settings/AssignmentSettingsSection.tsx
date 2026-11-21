import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useToast } from '../../app/ui/Toasts'
import { assignmentApi, type Priority, type StrategyOutcome } from '../assignment/api'
import { triageApi } from '../triage/api'
import { Card, ErrorNote, Pill, Spinner } from '../triage/ui'
import { percent, title as label } from '../triage/format'

/** Strategy pattern, live: pick the active assignment strategy and compare what each would choose. */
export function AssignmentSettingsSection({ canEdit }: { canEdit: boolean }) {
  const toast = useToast()
  const qc = useQueryClient()
  const settings = useQuery({ queryKey: ['assignment-settings'], queryFn: assignmentApi.settings })
  const options = useQuery({ queryKey: ['triage-options'], queryFn: triageApi.options, staleTime: 5 * 60_000 })
  const [category, setCategory] = useState('APPLICATION')
  const [service, setService] = useState('')
  const [priority, setPriority] = useState<Priority>('P1')

  const switchStrategy = useMutation({
    mutationFn: assignmentApi.switchStrategy,
    onSuccess: (s) => {
      qc.setQueryData(['assignment-settings'], s)
      const name = s.strategies.find((x) => x.active)?.displayName ?? s.activeStrategy
      toast(`Assignment strategy is now ${name}. The next incident uses it.`, 'success')
    },
    onError: (e) => toast(e instanceof Error ? e.message : 'Could not switch strategy.', 'error'),
  })

  const compare = useMutation({
    mutationFn: () => assignmentApi.compare({
      categoryCode: category,
      serviceId: options.data?.services.find((s) => s.name === service)?.id ?? null,
      priority,
    }),
  })

  function onCompare(e: FormEvent) {
    e.preventDefault()
    compare.mutate()
  }

  return (
    <Card title="Technician assignment" subtitle="Switching takes effect immediately; no restart, no code change.">
      {settings.isPending && <Spinner />}
      {settings.isError && <ErrorNote error={settings.error} />}
      {settings.data && (
        <div className="grid sm:grid-cols-2 gap-3">
          {settings.data.strategies.map((s) => (
            <button key={s.key} type="button" disabled={!canEdit || s.active || switchStrategy.isPending}
              onClick={() => switchStrategy.mutate(s.key)}
              className={`text-left rounded-lg p-3 ring-1 transition ${s.active ? 'ring-2 ring-indigo-600 bg-indigo-50' : 'ring-slate-200 hover:ring-indigo-300'} disabled:cursor-default`}>
              <div className="flex items-center justify-between">
                <span className="font-medium text-sm">{s.displayName}</span>
                {s.active && <Pill tone="blue">Active</Pill>}
              </div>
              <p className="text-xs text-slate-600 mt-1">{s.description}</p>
            </button>
          ))}
        </div>
      )}

      <form onSubmit={onCompare} className="mt-5 border-t border-slate-100 pt-4">
        <div className="text-sm font-medium text-slate-800 mb-2">Compare strategies for an incident</div>
        <div className="flex flex-wrap gap-2">
          <select value={category} onChange={(e) => setCategory(e.target.value)} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
            {(options.data?.categories ?? ['APPLICATION']).map((c) => <option key={c} value={c}>{label(c)}</option>)}
          </select>
          <select value={service} onChange={(e) => setService(e.target.value)} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
            <option value="">Any service</option>
            {(options.data?.services ?? []).map((s) => <option key={s.name} value={s.name}>{s.name}</option>)}
          </select>
          <select value={priority} onChange={(e) => setPriority(e.target.value as Priority)} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
            {(['P1', 'P2', 'P3', 'P4'] as Priority[]).map((p) => <option key={p}>{p}</option>)}
          </select>
          <button type="submit" disabled={compare.isPending} className="rounded-md bg-indigo-600 text-white px-3 py-1.5 text-sm disabled:opacity-60">
            {compare.isPending ? 'Comparing…' : 'Compare'}
          </button>
        </div>
        {compare.isError && <div className="mt-3"><ErrorNote error={compare.error} /></div>}
        {compare.data && <CompareTable rows={compare.data} />}
      </form>
    </Card>
  )
}

function CompareTable({ rows }: { rows: StrategyOutcome[] }) {
  return (
    <table className="w-full text-sm mt-3">
      <thead><tr className="text-left text-xs text-slate-500"><th className="py-1.5">Strategy</th><th>Would pick</th><th>Score</th><th>Why</th></tr></thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.strategyKey} className={`border-t border-slate-100 ${r.active ? 'bg-indigo-50/50' : ''}`}>
            <td className="py-2 pr-2 font-medium">{r.displayName}{r.active && ' ★'}</td>
            <td className="pr-2">{r.pick?.name ?? <span className="text-red-600">{r.error}</span>}</td>
            <td className="pr-2">{r.pick ? percent(r.pick.score) : '—'}</td>
            <td className="text-xs text-slate-600">{r.pick?.reasons.join(' · ')}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
