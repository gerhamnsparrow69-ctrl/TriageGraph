import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { PageHeader } from '../../app/PageHeader'
import { useToast } from '../../app/ui/Toasts'
import { useAuth } from '../auth/AuthContext'
import { Card, ErrorNote, Spinner } from '../triage/ui'
import { assignmentApi, type QueueItem, type TechnicianWorkload } from './api'
import { QueueTable } from './QueueTable'
import { WorkloadGauge } from './WorkloadGauge'

/** Team lead view: everyone's load side by side, with reassignment that respects capacity rules. */
export default function TeamWorkloadPage() {
  const { user } = useAuth()
  const toast = useToast()
  const qc = useQueryClient()
  const team = useQuery({ queryKey: ['workload', 'team'], queryFn: assignmentApi.teamWorkload, refetchInterval: 30_000,
    enabled: user?.role === 'TEAM_LEAD' })
  const [moving, setMoving] = useState<QueueItem | null>(null)
  const [target, setTarget] = useState<number | ''>('')
  const [reason, setReason] = useState('')

  const reassign = useMutation({
    mutationFn: () => assignmentApi.reassign(moving!.incidentId, { technicianId: Number(target), reason, override: false }),
    onSuccess: () => {
      toast(`${moving?.code} reassigned.`, 'success')
      setMoving(null)
      setReason('')
      setTarget('')
      qc.invalidateQueries({ queryKey: ['workload'] })
    },
    onError: (e) => toast(e instanceof Error ? e.message : 'Reassignment failed.', 'error'),
  })

  if (user?.role !== 'TEAM_LEAD') {
    return <><PageHeader title="Team workload" /><p className="text-sm text-slate-600">This view is for team leads.</p></>
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (target === '' || !reason.trim()) {
      toast('Choose a technician and give a reason.', 'error')
      return
    }
    reassign.mutate()
  }

  const members: TechnicianWorkload[] = team.data ?? []
  return (
    <>
      <PageHeader title="Team workload" subtitle="Busiest first. Full technicians cannot receive more work." />
      {team.isPending && <Spinner />}
      {team.isError && <ErrorNote error={team.error} />}
      <div className="grid gap-4">
        {members.map((w) => (
          <Card key={w.technicianId} title={w.name} subtitle={w.skills.join(', ')}>
            <div className="max-w-sm mb-3"><WorkloadGauge w={w} compact /></div>
            <QueueTable items={w.queue} onReassign={(i) => setMoving(i)} />
          </Card>
        ))}
      </div>

      {moving && (
        <div className="fixed inset-0 bg-slate-900/40 grid place-items-center p-4 z-40" role="dialog" aria-modal="true">
          <form onSubmit={onSubmit} className="w-full max-w-md rounded-xl bg-white p-5 space-y-3">
            <h2 className="text-base font-semibold">Reassign {moving.code}</h2>
            <p className="text-sm text-slate-600">{moving.title}</p>
            <select value={target} onChange={(e) => setTarget(e.target.value === '' ? '' : Number(e.target.value))}
              className="w-full rounded-md border border-slate-300 px-2 py-2 text-sm">
              <option value="">Choose a technician…</option>
              {members.map((m) => (
                <option key={m.technicianId} value={m.technicianId} disabled={m.atCapacity || !m.available}>
                  {m.name} ({m.activeLoad}/{m.maxLoad}){m.atCapacity ? ' · full' : ''}{!m.available ? ' · unavailable' : ''}
                </option>
              ))}
            </select>
            <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={2} maxLength={500} placeholder="Reason (kept in the audit trail)"
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setMoving(null)} className="rounded-md ring-1 ring-slate-300 px-3 py-2 text-sm">Cancel</button>
              <button type="submit" disabled={reassign.isPending} className="rounded-md bg-teal-600 text-white px-3 py-2 text-sm disabled:opacity-60">
                {reassign.isPending ? 'Reassigning…' : 'Reassign'}
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  )
}
