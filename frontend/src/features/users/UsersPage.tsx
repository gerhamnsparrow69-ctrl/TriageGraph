import { useMemo, useState, type FormEvent } from 'react'
import type { Role, User } from '../../api/client'
import { PageHeader } from '../../app/PageHeader'
import { useToast } from '../../app/ui/Toasts'
import { useAuth } from '../auth/AuthContext'
import { useCreateUser, useSetActive, useUpdateProfile, useUsers, type NewUserInput } from './api'
import { primaryButton, readProblem, roleLabel, secondaryButton } from './helpers'
import { Modal, RoleBadge, SelectField, SkillsField, TextField } from './ui'

const ROLES = Object.keys(roleLabel) as Role[]
const isTechRole = (r?: Role) => r === 'TECHNICIAN' || r === 'TEAM_LEAD'

function CreateUserForm({ onDone }: { onDone: () => void }) {
  const create = useCreateUser()
  const toast = useToast()
  const [form, setForm] = useState<NewUserInput>({
    role: 'TECHNICIAN', name: '', email: '', password: '', department: '', skills: [], experienceYears: 2, maxLoad: 5,
  })
  const [fields, setFields] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const set = <K extends keyof NewUserInput>(k: K, v: NewUserInput[K]) => setForm((f) => ({ ...f, [k]: v }))
  const tech = isTechRole(form.role)

  function submit(e: FormEvent) {
    e.preventDefault()
    setFields({})
    setError(null)
    const body: NewUserInput = tech
      ? { ...form, department: form.department || undefined }
      : { ...form, department: form.department || undefined, skills: [], experienceYears: 0, maxLoad: 0 }
    create.mutate(body, {
      onSuccess: (u) => {
        toast(`${u.name} added as ${roleLabel[u.role ?? 'EMPLOYEE'].toLowerCase()}`, 'success')
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
      <SelectField label="Role" value={form.role} onChange={(e) => set('role', e.target.value as Role)}>
        {ROLES.map((r) => <option key={r} value={r}>{roleLabel[r]}</option>)}
      </SelectField>
      <TextField label="Full name" required value={form.name} onChange={(e) => set('name', e.target.value)} error={fields.name} />
      <TextField label="Email" type="email" required value={form.email} onChange={(e) => set('email', e.target.value)} error={fields.email} />
      <TextField label="Temporary password" type="password" autoComplete="new-password" required value={form.password}
        onChange={(e) => set('password', e.target.value)} error={fields.password}
        hint="At least 8 characters, with a letter and a digit. Share it with the person securely." />
      <TextField label="Department (optional)" value={form.department ?? ''} onChange={(e) => set('department', e.target.value)}
        error={fields.department} />
      {tech && (
        <>
          <SkillsField value={form.skills ?? []} onChange={(v) => set('skills', v)} error={fields.skills} />
          <div className="grid grid-cols-2 gap-3">
            <TextField label="Years of experience" type="number" min={0} max={60} value={form.experienceYears}
              onChange={(e) => set('experienceYears', Number(e.target.value))} error={fields.experienceYears} />
            <TextField label="Max open incidents" type="number" min={1} max={50} value={form.maxLoad}
              onChange={(e) => set('maxLoad', Number(e.target.value))} error={fields.maxLoad} />
          </div>
          <p className="text-xs text-slate-500">Set availability and team lead on the Technicians page after creating.</p>
        </>
      )}
      {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
      <div className="flex justify-end gap-2 pt-1">
        <button type="button" onClick={onDone} className={secondaryButton}>Cancel</button>
        <button type="submit" disabled={create.isPending} className={primaryButton}>
          {create.isPending ? 'Adding…' : 'Add user'}
        </button>
      </div>
    </form>
  )
}

function EditUserForm({ user, onDone }: { user: User; onDone: () => void }) {
  const update = useUpdateProfile()
  const toast = useToast()
  const [form, setForm] = useState({ name: user.name ?? '', department: user.department ?? '', pushTopic: user.pushTopic ?? '' })
  const [fields, setFields] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)

  function submit(e: FormEvent) {
    e.preventDefault()
    setFields({})
    setError(null)
    update.mutate(
      { id: user.id!, name: form.name, department: form.department || undefined, pushTopic: form.pushTopic || undefined },
      {
        onSuccess: () => {
          toast('Saved', 'success')
          onDone()
        },
        onError: (err) => {
          const p = readProblem(err)
          setFields(p.fields)
          setError(p.message)
        },
      },
    )
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      <p className="text-sm text-slate-500">{user.email} · <RoleBadge role={user.role} /></p>
      <TextField label="Full name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} error={fields.name} />
      <TextField label="Department" value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })}
        error={fields.department} />
      <TextField label="Push topic (ntfy)" value={form.pushTopic} onChange={(e) => setForm({ ...form, pushTopic: e.target.value })}
        error={fields.pushTopic} hint="Phone alerts go to this ntfy topic. Use something long and hard to guess." />
      {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
      <div className="flex justify-end gap-2 pt-1">
        <button type="button" onClick={onDone} className={secondaryButton}>Cancel</button>
        <button type="submit" disabled={update.isPending} className={primaryButton}>{update.isPending ? 'Saving…' : 'Save'}</button>
      </div>
    </form>
  )
}

export default function UsersPage() {
  const { user: me } = useAuth()
  const canEdit = me?.role === 'ADMIN'
  const { data: users = [], isPending, isError } = useUsers()
  const setActive = useSetActive()
  const toast = useToast()
  const [query, setQuery] = useState('')
  const [role, setRole] = useState<Role | ''>('')
  const [showInactive, setShowInactive] = useState(true)
  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState<User | null>(null)

  const counts = useMemo(() => {
    const c: Partial<Record<Role, number>> = {}
    users.forEach((u) => u.role && (c[u.role] = (c[u.role] ?? 0) + 1))
    return c
  }, [users])

  const shown = users
    .filter((u) => !role || u.role === role)
    .filter((u) => showInactive || u.active)
    .filter((u) => {
      const q = query.trim().toLowerCase()
      return !q || [u.name, u.email, u.department].some((v) => v?.toLowerCase().includes(q))
    })
    .sort((a, b) => (a.name ?? '').localeCompare(b.name ?? ''))

  function toggleActive(u: User) {
    setActive.mutate(
      { id: u.id!, active: !u.active },
      {
        onSuccess: (r) => toast(`${r.name} ${r.active ? 'reactivated' : 'deactivated'}`, 'success'),
        onError: (err) => toast(readProblem(err).message ?? 'Could not change the account.', 'error'),
      },
    )
  }

  return (
    <>
      <PageHeader
        title="Users"
        subtitle={canEdit ? 'Everyone who can sign in. Only administrators can add or change accounts.' : 'Everyone who can sign in (read only).'}
        actions={canEdit && <button onClick={() => setCreating(true)} className={primaryButton}>Add user</button>}
      />

      <div className="flex flex-wrap items-center gap-2 mb-4">
        <input type="search" placeholder="Search name, email or department" value={query} onChange={(e) => setQuery(e.target.value)}
          aria-label="Search users"
          className="flex-1 min-w-[12rem] rounded-md border border-slate-300 px-3 py-2 text-sm bg-white outline-none focus:ring-2 focus:ring-indigo-500" />
        <select value={role} onChange={(e) => setRole(e.target.value as Role | '')} aria-label="Filter by role"
          className="rounded-md border border-slate-300 px-3 py-2 text-sm bg-white">
          <option value="">All roles ({users.length})</option>
          {ROLES.map((r) => <option key={r} value={r}>{roleLabel[r]} ({counts[r] ?? 0})</option>)}
        </select>
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input type="checkbox" checked={showInactive} onChange={(e) => setShowInactive(e.target.checked)} />
          Show deactivated
        </label>
      </div>

      {isPending && <p className="text-sm text-slate-500">Loading…</p>}
      {isError && <p className="text-sm text-red-600">Could not load users.</p>}
      {!isPending && !isError && (
        <div className="relative rounded-xl bg-white ring-1 ring-slate-200 overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-4 py-2.5 font-medium">Name</th>
                <th className="px-4 py-2.5 font-medium">Role</th>
                <th className="px-4 py-2.5 font-medium hidden md:table-cell">Department</th>
                <th className="px-4 py-2.5 font-medium">Status</th>
                {canEdit && <th className="px-4 py-2.5"><span className="sr-only">Actions</span></th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {shown.map((u) => (
                <tr key={u.id} className={u.active ? '' : 'bg-slate-50/60 text-slate-400'}>
                  <td className="px-4 py-2.5">
                    <div className={`font-medium ${u.active ? 'text-slate-900' : ''}`}>{u.name}</div>
                    <div className="text-xs text-slate-500">{u.email}</div>
                  </td>
                  <td className="px-4 py-2.5"><RoleBadge role={u.role} /></td>
                  <td className="px-4 py-2.5 hidden md:table-cell">{u.department ?? '–'}</td>
                  <td className="px-4 py-2.5">
                    <span className={`inline-flex items-center gap-1.5 text-xs ${u.active ? 'text-emerald-700' : 'text-slate-500'}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${u.active ? 'bg-emerald-500' : 'bg-slate-400'}`} />
                      {u.active ? 'Active' : 'Deactivated'}
                    </span>
                  </td>
                  {canEdit && (
                    <td className="px-4 py-2.5 text-right whitespace-nowrap">
                      <button onClick={() => setEditing(u)} className="text-indigo-600 hover:underline text-sm mr-3">Edit</button>
                      {u.id !== me?.id && (
                        <button onClick={() => toggleActive(u)} disabled={setActive.isPending}
                          className={`text-sm hover:underline ${u.active ? 'text-red-600' : 'text-emerald-700'}`}>
                          {u.active ? 'Deactivate' : 'Reactivate'}
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
              {shown.length === 0 && (
                <tr><td colSpan={5} className="px-4 py-8 text-center text-slate-500">No users match.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {creating && <Modal title="Add user" onClose={() => setCreating(false)}><CreateUserForm onDone={() => setCreating(false)} /></Modal>}
      {editing && <Modal title={`Edit ${editing.name}`} onClose={() => setEditing(null)}><EditUserForm user={editing} onDone={() => setEditing(null)} /></Modal>}
    </>
  )
}
