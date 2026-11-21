import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState, type FormEvent } from 'react'
import { useToast } from '../../app/ui/Toasts'
import { triageApi, type KeywordRule, type KeywordTarget } from '../triage/api'
import { Card, ErrorNote, Spinner } from '../triage/ui'
import { title as label } from '../triage/format'

/** Keyword rules and service aliases used by the rule-based classifier. Changes apply to the next preview. */
export function KeywordsSection({ canEdit }: { canEdit: boolean }) {
  const toast = useToast()
  const qc = useQueryClient()
  const rules = useQuery({ queryKey: ['triage-keywords'], queryFn: triageApi.keywords })
  const options = useQuery({ queryKey: ['triage-options'], queryFn: triageApi.options, staleTime: 5 * 60_000 })
  const [target, setTarget] = useState<KeywordTarget>('CATEGORY')
  const [key, setKey] = useState('')
  const [phrase, setPhrase] = useState('')
  const [filter, setFilter] = useState('')

  const grouped = useMemo(() => {
    const map = new Map<string, KeywordRule[]>()
    for (const r of rules.data ?? []) {
      if (filter && !`${r.key} ${r.phrase}`.toLowerCase().includes(filter.toLowerCase())) continue
      const k = `${r.target}:${r.key}`
      map.set(k, [...(map.get(k) ?? []), r])
    }
    return [...map.entries()]
  }, [rules.data, filter])

  const add = useMutation({
    mutationFn: () => triageApi.addKeyword({ target, key, phrase }),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ['triage-keywords'] })
      setPhrase('')
      toast(`'${r.phrase}' now points to ${r.key}.`, 'success')
    },
    onError: (e) => toast(e instanceof Error ? e.message : 'Could not add the keyword.', 'error'),
  })
  const remove = useMutation({
    mutationFn: (id: number) => triageApi.removeKeyword(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['triage-keywords'] }),
    onError: (e) => toast(e instanceof Error ? e.message : 'Could not remove the keyword.', 'error'),
  })

  const keys = target === 'CATEGORY' ? options.data?.categories ?? [] : (options.data?.services ?? []).map((s) => s.name)

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!key || !phrase.trim()) {
      toast('Choose a target and type a phrase.', 'error')
      return
    }
    add.mutate()
  }

  return (
    <Card title="Keyword rules" subtitle="Phrases that point to a category or a service (aliases like “UPI” → Fee Payment App)."
      actions={<input value={filter} onChange={(e) => setFilter(e.target.value)} placeholder="Filter…"
        className="rounded-md border border-slate-300 px-2 py-1 text-sm w-40" />}>
      {canEdit && (
        <form onSubmit={onSubmit} className="flex flex-wrap gap-2 mb-4">
          <select value={target} onChange={(e) => { setTarget(e.target.value as KeywordTarget); setKey('') }}
            className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
            <option value="CATEGORY">Category</option><option value="SERVICE">Service</option>
          </select>
          <select value={key} onChange={(e) => setKey(e.target.value)} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
            <option value="">Choose…</option>
            {keys.map((k) => <option key={k} value={k}>{target === 'CATEGORY' ? label(k) : k}</option>)}
          </select>
          <input value={phrase} onChange={(e) => setPhrase(e.target.value)} maxLength={60} placeholder="phrase, e.g. smartboard"
            className="rounded-md border border-slate-300 px-2 py-1.5 text-sm flex-1 min-w-[10rem]" />
          <button type="submit" disabled={add.isPending} className="rounded-md bg-indigo-600 text-white px-3 py-1.5 text-sm disabled:opacity-60">Add</button>
        </form>
      )}
      {rules.isPending && <Spinner />}
      {rules.isError && <ErrorNote error={rules.error} />}
      <div className="space-y-2 max-h-96 overflow-y-auto pr-1">
        {grouped.map(([k, list]) => (
          <div key={k} className="text-sm">
            <span className="font-medium text-slate-800">{list[0].target === 'CATEGORY' ? label(list[0].key) : list[0].key}</span>
            <span className="text-xs text-slate-400 ml-1">{list[0].target.toLowerCase()}</span>
            <div className="flex flex-wrap gap-1 mt-1">
              {list.map((r) => (
                <span key={r.id} className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-xs">
                  {r.phrase}
                  {canEdit && (
                    <button type="button" aria-label={`Remove ${r.phrase}`} onClick={() => remove.mutate(r.id)}
                      className="text-slate-400 hover:text-red-600">×</button>
                  )}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>
    </Card>
  )
}
