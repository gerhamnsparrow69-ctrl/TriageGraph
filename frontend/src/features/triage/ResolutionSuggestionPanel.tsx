import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useToast } from '../../app/ui/Toasts'
import { triageApi } from './api'
import { Card, ErrorNote, Pill, Spinner } from './ui'

/**
 * Suggested resolution (Intelligent Feature G): the closest past fix, or an AI-drafted plan grounded in the
 * similar incidents when Gemini is configured. `onUseDraft` lets Member 1's resolve form prefill itself.
 */
export function ResolutionSuggestionPanel({ incidentId, onUseDraft }: { incidentId: number; onUseDraft?: (draft: string) => void }) {
  const toast = useToast()
  const [useAi, setUseAi] = useState(true)
  const q = useQuery({
    queryKey: ['resolution-suggestion', incidentId, useAi],
    queryFn: () => triageApi.resolutionSuggestion(incidentId, useAi),
    retry: false,
  })

  async function copy(text: string) {
    try {
      await navigator.clipboard.writeText(text)
      toast('Draft copied.', 'success')
    } catch {
      toast('Could not copy; select the text instead.', 'error')
    }
  }

  return (
    <Card title="Suggested resolution"
      actions={
        <label className="flex items-center gap-1.5 text-xs text-slate-600">
          <input type="checkbox" checked={useAi} onChange={(e) => setUseAi(e.target.checked)} /> AI draft
        </label>
      }>
      {q.isPending && <Spinner label="Looking at past fixes…" />}
      {q.isError && <ErrorNote error={q.error} />}
      {q.data && (
        <>
          <div className="mb-2">
            {q.data.aiGenerated ? <Pill tone="violet">Drafted by Gemini from past fixes</Pill> : <Pill>From the closest past fix</Pill>}
          </div>
          <pre className="whitespace-pre-wrap font-sans text-sm text-slate-800 bg-slate-50 rounded-md p-3">{q.data.draft}</pre>
          {q.data.basedOn.length > 0 && (
            <div className="text-xs text-slate-500 mt-2">Based on {q.data.basedOn.map((b) => b.code).join(', ')}</div>
          )}
          <div className="flex gap-3 mt-3">
            {onUseDraft && (
              <button type="button" onClick={() => onUseDraft(q.data.draft)}
                className="rounded-md bg-teal-600 text-white px-3 py-1.5 text-sm hover:bg-teal-700">Use as draft</button>
            )}
            <button type="button" onClick={() => copy(q.data.draft)}
              className="rounded-md ring-1 ring-slate-300 px-3 py-1.5 text-sm hover:bg-slate-50">Copy</button>
          </div>
        </>
      )}
    </Card>
  )
}
