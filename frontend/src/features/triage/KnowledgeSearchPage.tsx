import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { PageHeader } from '../../app/PageHeader'
import { triageApi } from './api'
import { Card, ErrorNote, Pill } from './ui'
import { percent } from './format'

/** Technicians describe a problem in their own words and get the closest past fixes (semantic search). */
export default function KnowledgeSearchPage() {
  const [query, setQuery] = useState('')
  const [useAi, setUseAi] = useState(false)
  const search = useMutation({ mutationFn: () => triageApi.searchKnowledge(query.trim(), useAi) })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (query.trim().length >= 3) search.mutate()
  }

  return (
    <>
      <PageHeader title="Knowledge base" subtitle="Describe the symptom; past incidents are matched by meaning." />
      <form onSubmit={onSubmit} className="rounded-xl bg-white ring-1 ring-slate-200 p-4 mb-5">
        <textarea value={query} onChange={(e) => setQuery(e.target.value)} rows={3} maxLength={1000}
          placeholder="e.g. payment gateway times out with TLS handshake errors"
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-teal-500" />
        <div className="flex items-center justify-between mt-3">
          <label className="flex items-center gap-1.5 text-sm text-slate-600">
            <input type="checkbox" checked={useAi} onChange={(e) => setUseAi(e.target.checked)} /> Ask Gemini for a step-by-step draft
          </label>
          <button type="submit" disabled={search.isPending || query.trim().length < 3}
            className="rounded-md bg-teal-600 text-white px-4 py-2 text-sm font-medium hover:bg-teal-700 disabled:opacity-60">
            {search.isPending ? 'Searching…' : 'Search'}
          </button>
        </div>
      </form>
      {search.isError && <ErrorNote error={search.error} />}
      {search.data && (
        <div className="grid xl:grid-cols-[1fr_24rem] gap-5">
          <Card title="Closest past incidents">
            {search.data.hits.length === 0 && <p className="text-sm text-slate-500">No resolved incident matches yet.</p>}
            <ul className="divide-y divide-slate-100">
              {search.data.hits.map((h) => (
                <li key={h.code} className="py-3">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-sm font-medium">{h.code} · {h.title}</span>
                    <Pill tone="violet">{percent(h.similarity)}</Pill>
                  </div>
                  <div className="text-xs text-slate-500 mt-0.5">{[h.categoryCode, h.serviceName].filter(Boolean).join(' · ')}</div>
                  {h.resolutionSummary && <p className="text-sm text-slate-700 mt-1">Fix: {h.resolutionSummary}</p>}
                  {h.rootCause && <p className="text-xs text-slate-500 mt-0.5">Root cause: {h.rootCause}</p>}
                </li>
              ))}
            </ul>
          </Card>
          <Card title="Suggested approach">
            <div className="mb-2">{search.data.suggestion.aiGenerated ? <Pill tone="violet">Gemini draft</Pill> : <Pill>Closest fix</Pill>}</div>
            <pre className="whitespace-pre-wrap font-sans text-sm text-slate-800">{search.data.suggestion.draft}</pre>
          </Card>
        </div>
      )}
    </>
  )
}
