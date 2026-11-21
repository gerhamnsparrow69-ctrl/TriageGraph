import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useToast } from '../../app/ui/Toasts'
import { triageApi, type TriageSettings } from '../triage/api'
import { Card, ErrorNote, Spinner } from '../triage/ui'

/** Duplicate thresholds, search window and the AI switch, plus what the engine is running right now. */
export function TriageSettingsSection({ canEdit }: { canEdit: boolean }) {
  const toast = useToast()
  const qc = useQueryClient()
  const settings = useQuery({ queryKey: ['triage-settings'], queryFn: triageApi.settings })
  const status = useQuery({ queryKey: ['triage-status'], queryFn: triageApi.status, refetchInterval: 30_000 })
  return (
    <Card title="Duplicate detection and AI" subtitle="score = 0.80 × meaning similarity + 0.10 × same service + 0.10 × recency">
      {settings.isPending && <Spinner />}
      {settings.isError && <ErrorNote error={settings.error} />}
      {settings.data && (
        <SettingsForm key={settings.data.updatedAt} initial={settings.data} canEdit={canEdit}
          onSaved={(s) => {
            qc.setQueryData(['triage-settings'], s)
            qc.invalidateQueries({ queryKey: ['triage-evaluation'] })
            qc.invalidateQueries({ queryKey: ['triage-status'] })
            toast('Triage settings saved.', 'success')
          }}
          onError={(e) => toast(e instanceof Error ? e.message : 'Could not save.', 'error')} />
      )}
      {status.data && (
        <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs text-slate-600 mt-5 border-t border-slate-100 pt-3">
          <dt>Embedding model</dt><dd className="text-slate-900">{status.data.embedder}</dd>
          <dt>Knowledge base</dt><dd className="text-slate-900">{status.data.archiveEntries} past fixes · {status.data.liveEntries} live ({status.data.openLive} open)</dd>
          <dt>LLM</dt><dd className="text-slate-900 break-words">{status.data.llm}</dd>
        </dl>
      )}
    </Card>
  )
}

/** Holds the editable copy; re-mounted (by key) whenever the saved settings change. */
function SettingsForm({ initial, canEdit, onSaved, onError }: {
  initial: TriageSettings
  canEdit: boolean
  onSaved: (s: TriageSettings) => void
  onError: (e: unknown) => void
}) {
  const [likely, setLikely] = useState(initial.likelyThreshold)
  const [possibly, setPossibly] = useState(initial.possiblyThreshold)
  const [hours, setHours] = useState(initial.windowHours)
  const [ai, setAi] = useState(initial.aiEnabled)
  const save = useMutation({
    mutationFn: () => triageApi.updateSettings({ likelyThreshold: likely, possiblyThreshold: possibly, windowHours: hours, aiEnabled: ai }),
    onSuccess: onSaved,
    onError,
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    save.mutate()
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <Slider label="Likely duplicate at" value={likely} onChange={setLikely} disabled={!canEdit}
        hint="Shows the 'join this incident' banner. Calibrated value: 0.62." />
      <Slider label="Possibly related at" value={possibly} onChange={setPossibly} disabled={!canEdit}
        hint="Shown as a side list only." />
      <label className="flex items-center gap-3 text-sm">
        <span className="w-44 text-slate-700">Look back</span>
        <input type="number" min={1} max={720} value={hours} disabled={!canEdit}
          onChange={(e) => setHours(Math.max(1, Math.min(720, Number(e.target.value) || 1)))}
          className="w-24 rounded-md border border-slate-300 px-2 py-1 text-sm" /> hours
      </label>
      <label className="flex items-start gap-3 text-sm">
        <input type="checkbox" checked={ai} disabled={!canEdit} onChange={(e) => setAi(e.target.checked)} className="mt-1" />
        <span>
          Use Gemini for classification and resolution drafts
          {!initial.llmConfigured && (
            <span className="block text-xs text-amber-700">No GEMINI_API_KEY is configured, so local classifiers are used either way.</span>
          )}
        </span>
      </label>
      {canEdit && (
        <button type="submit" disabled={save.isPending} className="rounded-md bg-indigo-600 text-white px-4 py-2 text-sm disabled:opacity-60">
          {save.isPending ? 'Saving…' : 'Save'}
        </button>
      )}
    </form>
  )
}

function Slider({ label, value, onChange, hint, disabled }: {
  label: string
  value: number
  onChange: (v: number) => void
  hint: string
  disabled: boolean
}) {
  return (
    <label className="block text-sm">
      <span className="flex items-center gap-3">
        <span className="w-44 text-slate-700">{label}</span>
        <input type="range" min={0.3} max={0.95} step={0.01} value={value} disabled={disabled}
          onChange={(e) => onChange(Number(e.target.value))} className="flex-1 accent-indigo-600" />
        <span className="w-12 text-right font-mono">{value.toFixed(2)}</span>
      </span>
      <span className="block text-xs text-slate-500 ml-[11.75rem]">{hint}</span>
    </label>
  )
}
