import { useState } from 'react'
import { ApiError } from '../../api/client'
import { useToast } from '../../app/ui/Toasts'
import { actionsFor, bodyFor, isComplete, type ActionSpec } from './actions'
import { useIncidentAction, useTechnicians } from './api'
import type { IncidentView } from './types'

const button = 'rounded-md px-3 py-1.5 text-sm font-medium disabled:opacity-50'
const primaryClass = `${button} bg-slate-900 text-white hover:bg-slate-700`
const secondaryClass = `${button} bg-white ring-1 ring-slate-300 text-slate-700 hover:bg-slate-50`
const field = 'w-full rounded-md ring-1 ring-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-500'

/** Renders exactly the actions the backend allows this user, plus the small form some of them need. */
export function ActionBar({ incident }: { incident: IncidentView }) {
  const specs = actionsFor(incident.allowedActions)
  const [open, setOpen] = useState<ActionSpec | null>(null)
  const [values, setValues] = useState<Record<string, string>>({})
  const mutation = useIncidentAction(incident.id)
  const toast = useToast()
  const technicians = useTechnicians(open?.input.kind === 'technician')

  if (specs.length === 0) return null

  const send = (spec: ActionSpec) =>
    mutation.mutate(
      { path: spec.path, body: bodyFor(spec, values) },
      {
        onSuccess: (updated) => {
          toast(`${incident.code}: ${spec.label.toLowerCase()} done. Now ${updated.status.replace('_', ' ').toLowerCase()}.`, 'success')
          setOpen(null)
          setValues({})
        },
        onError: (e) => toast(e instanceof ApiError ? e.message : 'Something went wrong.', 'error'),
      },
    )

  const start = (spec: ActionSpec) => {
    if (spec.input.kind === 'none') send(spec)
    else {
      setValues({})
      setOpen(spec)
    }
  }
  const set = (k: string) => (e: { target: { value: string } }) => setValues((v) => ({ ...v, [k]: e.target.value }))

  return (
    <section aria-label="Actions" className="rounded-xl bg-white ring-1 ring-slate-200 p-4">
      <div className="flex flex-wrap gap-2">
        {specs.map((s) => (
          <button key={s.action} onClick={() => start(s)} disabled={mutation.isPending}
            className={s.primary ? primaryClass : secondaryClass} aria-pressed={open?.action === s.action}>
            {s.label}
          </button>
        ))}
      </div>

      {open && (
        <form className="mt-4 space-y-3" onSubmit={(e) => { e.preventDefault(); send(open) }}>
          {open.input.kind === 'text' && (
            <label className="block text-sm">
              <span className="text-slate-700">{open.input.prompt}</span>
              <textarea className={`${field} mt-1`} rows={3} maxLength={1000} autoFocus
                placeholder={open.input.placeholder} value={values.text ?? ''} onChange={set('text')} />
            </label>
          )}
          {open.input.kind === 'resolve' && (
            <>
              <label className="block text-sm">
                <span className="text-slate-700">What fixed it?</span>
                <textarea className={`${field} mt-1`} rows={3} maxLength={2000} autoFocus
                  placeholder="Renewed the payment gateway's TLS certificate" value={values.summary ?? ''} onChange={set('summary')} />
              </label>
              <label className="block text-sm">
                <span className="text-slate-700">Root cause <span className="text-slate-400">(optional)</span></span>
                <input className={`${field} mt-1`} maxLength={1000} value={values.rootCause ?? ''} onChange={set('rootCause')} />
              </label>
            </>
          )}
          {open.input.kind === 'technician' && (
            <label className="block text-sm">
              <span className="text-slate-700">Assign to</span>
              <select className={`${field} mt-1`} value={values.technicianId ?? ''} onChange={set('technicianId')}>
                <option value="">{technicians.isPending ? 'Loading technicians…' : 'Choose a technician'}</option>
                {technicians.data
                  ?.filter((t) => t.id !== incident.assigneeId && t.technician?.available !== false)
                  .map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}{t.technician?.skills?.length ? ` (${t.technician.skills.join(', ')})` : ''}
                    </option>
                  ))}
              </select>
            </label>
          )}
          {open.input.kind === 'feedback' && (
            <>
              <fieldset className="text-sm">
                <legend className="text-slate-700">How did we do?</legend>
                <div className="flex gap-1 mt-1">
                  {[1, 2, 3, 4, 5].map((n) => (
                    <button type="button" key={n} aria-label={`${n} star${n > 1 ? 's' : ''}`}
                      onClick={() => setValues((v) => ({ ...v, rating: String(n) }))}
                      className={`text-2xl ${Number(values.rating) >= n ? 'text-amber-400' : 'text-slate-300'}`}>★</button>
                  ))}
                </div>
              </fieldset>
              <input className={field} placeholder="Anything to add? (optional)" maxLength={1000}
                value={values.comment ?? ''} onChange={set('comment')} />
            </>
          )}
          <div className="flex gap-2">
            <button type="submit" className={primaryClass} disabled={!isComplete(open, values) || mutation.isPending}>
              {mutation.isPending ? 'Sending…' : open.label}
            </button>
            <button type="button" className={secondaryClass} onClick={() => setOpen(null)}>Cancel</button>
          </div>
        </form>
      )}
    </section>
  )
}
