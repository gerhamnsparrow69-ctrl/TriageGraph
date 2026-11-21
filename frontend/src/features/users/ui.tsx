import { useEffect, useState, type ReactNode } from 'react'
import type { Role } from '../../api/client'
import { roleLabel } from './helpers'

const roleTone: Record<Role, string> = {
  EMPLOYEE: 'bg-blue-50 text-blue-700 ring-blue-200',
  TECHNICIAN: 'bg-teal-50 text-teal-700 ring-teal-200',
  TEAM_LEAD: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  ADMIN: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
  IT_MANAGER: 'bg-violet-50 text-violet-700 ring-violet-200',
}

export function RoleBadge({ role }: { role?: Role }) {
  if (!role) return null
  return <span className={`text-xs font-medium rounded-full px-2 py-0.5 ring-1 ${roleTone[role]}`}>{roleLabel[role]}</span>
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])
  return (
    <div className="fixed inset-0 z-40 bg-slate-900/40 grid place-items-center p-4" onClick={onClose}>
      <div role="dialog" aria-modal="true" aria-label={title} onClick={(e) => e.stopPropagation()}
        className="w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-xl bg-white shadow-xl ring-1 ring-slate-200">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h2 className="font-semibold text-slate-900">{title}</h2>
          <button type="button" onClick={onClose} aria-label="Close" className="text-slate-400 hover:text-slate-700 text-xl leading-none">×</button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  )
}

const inputClass = (error?: string) =>
  `w-full rounded-md border px-3 py-2 text-sm bg-white outline-none focus:ring-2 focus:ring-indigo-500 ${
    error ? 'border-red-400' : 'border-slate-300'
  }`

export function TextField({ label, error, hint, ...input }:
  { label: string; error?: string; hint?: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="block">
      <span className="block text-sm font-medium text-slate-700 mb-1">{label}</span>
      <input {...input} className={inputClass(error)} />
      {hint && !error && <span className="block text-xs text-slate-500 mt-1">{hint}</span>}
      {error && <span className="block text-xs text-red-600 mt-1">{error}</span>}
    </label>
  )
}

export function SelectField({ label, error, children, ...select }:
  { label: string; error?: string; children: ReactNode } & React.SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <label className="block">
      <span className="block text-sm font-medium text-slate-700 mb-1">{label}</span>
      <select {...select} className={inputClass(error)}>{children}</select>
      {error && <span className="block text-xs text-red-600 mt-1">{error}</span>}
    </label>
  )
}

/** Category codes from the brief; skills are free text, so admins can add new ones. */
const SUGGESTED_SKILLS = ['APPLICATION', 'AUTHENTICATION', 'CLOUD', 'DATABASE', 'HARDWARE', 'NETWORK', 'SECURITY', 'SOFTWARE', 'STORAGE']

export function SkillsField({ value, onChange, error }: { value: string[]; onChange: (v: string[]) => void; error?: string }) {
  const [draft, setDraft] = useState('')
  const add = (raw: string) => {
    const skill = raw.trim().toUpperCase().replace(/\s+/g, '_')
    if (skill && !value.includes(skill)) onChange([...value, skill])
    setDraft('')
  }
  return (
    <div>
      <span className="block text-sm font-medium text-slate-700 mb-1">Skills</span>
      <div className={`flex flex-wrap gap-1.5 rounded-md border px-2 py-1.5 bg-white ${error ? 'border-red-400' : 'border-slate-300'}`}>
        {value.map((s) => (
          <span key={s} className="inline-flex items-center gap-1 rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-700">
            {s}
            <button type="button" aria-label={`Remove ${s}`} onClick={() => onChange(value.filter((x) => x !== s))}
              className="text-slate-400 hover:text-red-600">×</button>
          </span>
        ))}
        <input list="skill-suggestions" value={draft} placeholder={value.length ? '' : 'Type a skill and press Enter'}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ',') {
              e.preventDefault()
              add(draft)
            } else if (e.key === 'Backspace' && !draft && value.length) {
              onChange(value.slice(0, -1))
            }
          }}
          onBlur={() => draft && add(draft)}
          className="flex-1 min-w-[8rem] text-sm outline-none py-0.5" />
        <datalist id="skill-suggestions">
          {SUGGESTED_SKILLS.filter((s) => !value.includes(s)).map((s) => <option key={s} value={s} />)}
        </datalist>
      </div>
      {error && <span className="block text-xs text-red-600 mt-1">{error}</span>}
    </div>
  )
}
