import { useState, type FormEvent } from 'react'
import type { User } from '../../api/client'
import { PageHeader } from '../../app/PageHeader'
import { useToast } from '../../app/ui/Toasts'
import { useAuth } from '../auth/AuthContext'
import { useTechnicians, useUpdateTechnician, type TechnicianInput } from './api'
import { primaryButton, readProblem, secondaryButton } from './helpers'
import { Modal, RoleBadge, SelectField, SkillsField, TextField } from './ui'

const inputOf = (u: User): TechnicianInput => ({
  skills: [...(u.technician?.skills ?? [])].sort(),
  experienceYears: u.technician?.experienceYears ?? 0,
  maxLoad: u.technician?.maxLoad ?? 1,
  available: u.technician?.available ?? true,
  teamLeadId: u.technician?.teamLeadId ?? null,
})

function EditTechnicianForm({ tech, leads, onDone }: { tech: User; leads: User[]; onDone: () => void }) {
  const update = useUpdateTechnician()
  const toast = useToast()
  const [form, setForm] = useState<TechnicianInput>(inputOf(tech))
  const [fields, setFields] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const set = <K extends keyof TechnicianInput>(k: K, v: TechnicianInput[K]) => setForm((f) => ({ ...f, [k]: v }))

  function submit(e: FormEvent) {
    e.preventDefault()
    setFields({})
    setError(null)
    update.mutate({ id: tech.id!, ...form }, {
      onSuccess: () => {
        toast(`${tech.name} updated`, 'success')
        onDone()
      },
      onError: (err) => {
        const p = readProblem(err)
        setFields(p.fields)
        setError(p.message)
      },
    })
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      <p className="text-sm text-slate-500">{tech.email} · <RoleBadge role={tech.role} /></p>
      <SkillsField value={form.skills} onChange={(v) => set('skills', v)} error={fields.skills} />
      <div className="grid grid-cols-2 gap-3">
        <TextField label="Years of experience" type="number" min={0} max={60} value={form.experienceYears}
          onChange={(e) => set('experienceYears', Number(e.target.value))} error={fields.experienceYears} />
        <TextField label="Max open incidents" type="number" min={1} max={50} value={form.maxLoad}
          onChange={(e) => set('maxLoad', Number(e.target.value))} error={fields.maxLoad} />
      </div>
      {tech.role === 'TECHNICIAN' && (
        <SelectField label="Team lead" value={form.teamLeadId ?? ''}
          onChange={(e) => set('teamLeadId', e.target.value ? Number(e.target.value) : null)}>
          <option value="">No team lead</option>
          {leads.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
        </SelectField>
      )}
      <label className="flex items-start gap-2 text-sm">
        <input type="checkbox" className="mt-0.5" checked={form.available} onChange={(e) => set('available', e.target.checked)} />
        <span>
          <span className="font-medium text-slate-700">Available for new incidents</span>
          <span className="block text-xs text-slate-500">Untick for leave or training; automatic assignment skips them.</span>
        </span>
      </label>
      {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
      <div className="flex justify-end gap-2 pt-1">
        <button type="button" onClick={onDone} className={secondaryButton}>Cancel</button>
        <button type="submit" disabled={update.isPending} className={primaryButton}>{update.isPending ? 'Saving…' : 'Save'}</button>
      </div>
    </form>
  )
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl bg-white ring-1 ring-slate-200 p-4">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className="text-2xl font-semibold text-slate-900 mt-1 tabular-nums">{value}</div>
    </div>
  )
}

export default function TechniciansPage() {
  const { user: me } = useAuth()
  const canEdit = me?.role === 'ADMIN'
  const { data = [], isPending, isError } = useTechnicians()
  const update = useUpdateTechnician()
  const toast = useToast()
  const [skill, setSkill] = useState('')
  const [editing, setEditing] = useState<User | null>(null)

  const techs = data.filter((t) => t.active).sort((a, b) => (a.name ?? '').localeCompare(b.name ?? ''))
  const leads = techs.filter((t) => t.role === 'TEAM_LEAD')
  const skills = [...new Set(techs.flatMap((t) => t.technician?.skills ?? []))].sort()
  const shown = techs.filter((t) => !skill || t.technician?.skills?.includes(skill))

  function toggleAvailable(t: User) {
    const next = { ...inputOf(t), available: !t.technician?.available }
    update.mutate({ id: t.id!, ...next }, {
      onSuccess: (r) => toast(`${r.name} is now ${r.technician?.available ? 'available' : 'unavailable'}`, 'success'),
      onError: (err) => toast(readProblem(err).message ?? 'Could not update.', 'error'),
    })
  }

  return (
    <>
      <PageHeader title="Technicians" subtitle="Skills, capacity and teams used by automatic assignment. Add new technicians on the Users page." />

      <div className="grid grid-cols-3 gap-4 mb-6">
        <Stat label="Technicians" value={techs.length} />
        <Stat label="Available now" value={techs.filter((t) => t.technician?.available).length} />
        <Stat label="Team leads" value={leads.length} />
      </div>

      <div className="flex items-center gap-2 mb-4">
        <select value={skill} onChange={(e) => setSkill(e.target.value)} aria-label="Filter by skill"
          className="rounded-md border border-slate-300 px-3 py-2 text-sm bg-white">
          <option value="">All skills</option>
          {skills.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      {isPending && <p className="text-sm text-slate-500">Loading…</p>}
      {isError && <p className="text-sm text-red-600">Could not load technicians.</p>}
      {!isPending && !isError && (
        <div className="relative rounded-xl bg-white ring-1 ring-slate-200 overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-4 py-2.5 font-medium">Name</th>
                <th className="px-4 py-2.5 font-medium">Skills</th>
                <th className="px-4 py-2.5 font-medium text-right">Experience</th>
                <th className="px-4 py-2.5 font-medium text-right">Max load</th>
                <th className="px-4 py-2.5 font-medium hidden md:table-cell">Team lead</th>
                <th className="px-4 py-2.5 font-medium">Availability</th>
                {canEdit && <th className="px-4 py-2.5"><span className="sr-only">Actions</span></th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {shown.map((t) => {
                const d = t.technician
                return (
                  <tr key={t.id}>
                    <td className="px-4 py-2.5">
                      <div className="font-medium text-slate-900">{t.name}</div>
                      <div className="mt-0.5"><RoleBadge role={t.role} /></div>
                    </td>
                    <td className="px-4 py-2.5">
                      <div className="flex flex-wrap gap-1 max-w-xs">
                        {[...(d?.skills ?? [])].sort().map((s) => (
                          <span key={s} className="rounded bg-slate-100 px-1.5 py-0.5 text-[11px] text-slate-700">{s}</span>
                        ))}
                      </div>
                    </td>
                    <td className="px-4 py-2.5 text-right tabular-nums">{d?.experienceYears ?? 0} yrs</td>
                    <td className="px-4 py-2.5 text-right tabular-nums">{d?.maxLoad ?? '–'}</td>
                    <td className="px-4 py-2.5 hidden md:table-cell">{t.role === 'TEAM_LEAD' ? <span className="text-slate-400">leads a team</span> : (d?.teamLeadName ?? '–')}</td>
                    <td className="px-4 py-2.5">
                      {canEdit ? (
                        <button onClick={() => toggleAvailable(t)} disabled={update.isPending} role="switch" aria-checked={!!d?.available}
                          aria-label={`${t.name} available`}
                          className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${d?.available ? 'bg-emerald-500' : 'bg-slate-300'}`}>
                          <span className={`inline-block h-4 w-4 rounded-full bg-white shadow transition-transform ${d?.available ? 'translate-x-4' : 'translate-x-0.5'}`} />
                        </button>
                      ) : (
                        <span className={d?.available ? 'text-emerald-700' : 'text-slate-500'}>{d?.available ? 'Available' : 'Away'}</span>
                      )}
                    </td>
                    {canEdit && (
                      <td className="px-4 py-2.5 text-right">
                        <button onClick={() => setEditing(t)} className="text-indigo-600 hover:underline">Edit</button>
                      </td>
                    )}
                  </tr>
                )
              })}
              {shown.length === 0 && (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-slate-500">No technicians match.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {editing && (
        <Modal title={`Edit ${editing.name}`} onClose={() => setEditing(null)}>
          <EditTechnicianForm tech={editing} leads={leads.filter((l) => l.id !== editing.id)} onDone={() => setEditing(null)} />
        </Modal>
      )}
    </>
  )
}
