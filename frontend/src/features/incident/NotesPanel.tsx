import { useState } from 'react'
import { ApiError } from '../../api/client'
import { useToast } from '../../app/ui/Toasts'
import { useIncidentAction } from './api'
import { formatIst } from './format'
import type { IncidentView } from './types'

/** Comments and work notes. Internal notes are staff-only; the backend already hides them from employees. */
export function NotesPanel({ incident, isStaff }: { incident: IncidentView; isStaff: boolean }) {
  const [text, setText] = useState('')
  const [internal, setInternal] = useState(false)
  const mutation = useIncidentAction(incident.id)
  const toast = useToast()
  const canAdd = incident.allowedActions.includes('ADD_NOTE')

  const submit = () =>
    mutation.mutate(
      { path: 'notes', body: { text: text.trim(), internal: isStaff && internal } },
      {
        onSuccess: () => { setText(''); setInternal(false) },
        onError: (e) => toast(e instanceof ApiError ? e.message : 'Could not add the note.', 'error'),
      },
    )

  return (
    <section aria-label="Notes" className="rounded-xl bg-white ring-1 ring-slate-200 p-4">
      <h2 className="font-semibold text-slate-900 mb-3">Notes and comments</h2>
      {incident.notes.length === 0 && <p className="text-sm text-slate-500">No notes yet.</p>}
      <ul className="space-y-3">
        {incident.notes.map((n) => (
          <li key={n.id} className={`rounded-lg p-3 text-sm ${n.internal ? 'bg-amber-50 ring-1 ring-amber-200' : 'bg-slate-50'}`}>
            <div className="flex justify-between gap-2 text-xs text-slate-500 mb-1">
              <span>
                {n.authorId === incident.reporterId ? 'Reporter' : n.authorId === incident.assigneeId ? 'Assigned technician' : `User #${n.authorId ?? 'system'}`}
                {n.internal && <span className="ml-2 font-semibold text-amber-700">Internal</span>}
              </span>
              <time dateTime={n.createdAt}>{formatIst(n.createdAt)}</time>
            </div>
            <p className="whitespace-pre-wrap text-slate-800">{n.text}</p>
          </li>
        ))}
      </ul>
      {canAdd && (
        <form className="mt-4 space-y-2" onSubmit={(e) => { e.preventDefault(); submit() }}>
          <textarea aria-label="New note" rows={2} maxLength={2000} value={text} onChange={(e) => setText(e.target.value)}
            placeholder="Add a comment…"
            className="w-full rounded-md ring-1 ring-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-500" />
          <div className="flex items-center justify-between">
            {isStaff ? (
              <label className="text-sm text-slate-600 flex items-center gap-2">
                <input type="checkbox" checked={internal} onChange={(e) => setInternal(e.target.checked)} />
                Internal (IT staff only)
              </label>
            ) : <span />}
            <button type="submit" disabled={!text.trim() || mutation.isPending}
              className="rounded-md bg-slate-900 text-white px-3 py-1.5 text-sm font-medium disabled:opacity-50">
              Add note
            </button>
          </div>
        </form>
      )}
    </section>
  )
}
