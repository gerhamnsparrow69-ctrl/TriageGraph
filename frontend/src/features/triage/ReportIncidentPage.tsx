import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'
import { useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { ApiError } from '../../api/client'
import { PageHeader } from '../../app/PageHeader'
import { useToast } from '../../app/ui/Toasts'
import { triageApi, type DuplicateView, type PreviewResponse } from './api'
import { useDebounced, useSpeechInput } from './hooks'
import { submitIncident, type Impact, type Urgency } from './incidentApi'
import { Meter, Pill } from './ui'
import { minutesAgo, percent, sourceLabel, title as label } from './format'

const IMPACTS: { value: Impact; text: string }[] = [
  { value: 'MINOR', text: 'Just me' },
  { value: 'MODERATE', text: 'A few people' },
  { value: 'SIGNIFICANT', text: 'A department or hostel block' },
  { value: 'EXTENSIVE', text: 'The whole campus' },
]
const URGENCIES: { value: Urgency; text: string }[] = [
  { value: 'LOW', text: 'Can wait' },
  { value: 'MEDIUM', text: 'Slows my work' },
  { value: 'HIGH', text: 'Blocks my work' },
  { value: 'CRITICAL', text: 'Blocks critical work now' },
]

/** Employee report form with live AI triage: suggestions and duplicate detection while typing. */
export default function ReportIncidentPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const [titleText, setTitleText] = useState('')
  const [description, setDescription] = useState('')
  const [impact, setImpact] = useState<Impact>('MODERATE')
  const [urgency, setUrgency] = useState<Urgency>('MEDIUM')
  const [affectedUsers, setAffectedUsers] = useState(1)
  const [category, setCategory] = useState<string | null>(null) // null = use the suggestion
  const [serviceName, setServiceName] = useState<string | null>(null)
  const [dismissed, setDismissed] = useState<string[]>([])
  const [showWhy, setShowWhy] = useState(false)

  const speech = useSpeechInput((t) => setDescription((d) => (d ? `${d} ${t}` : t)))
  const options = useQuery({ queryKey: ['triage-options'], queryFn: triageApi.options, staleTime: 5 * 60_000 })

  const chosenServiceId = useMemo(() => {
    if (serviceName === null) return null
    return options.data?.services.find((s) => s.name === serviceName)?.id ?? null
  }, [serviceName, options.data])

  const debounced = useDebounced({ t: titleText.trim(), d: description.trim(), s: chosenServiceId }, 600)
  const ready = debounced.t.length + debounced.d.length >= 8
  const preview = useQuery({
    queryKey: ['triage-preview', debounced],
    queryFn: () => triageApi.preview({ title: debounced.t, description: debounced.d, serviceId: debounced.s }),
    enabled: ready,
    placeholderData: keepPreviousData,
    retry: false,
  })
  const p: PreviewResponse | undefined = ready ? preview.data : undefined

  const effectiveCategory = category ?? p?.suggestion.categoryCode ?? ''
  const effectiveService = serviceName ?? p?.suggestedServiceName ?? ''
  const duplicate = p?.likelyDuplicates.find((d) => !dismissed.includes(d.code))

  const join = useMutation({
    mutationFn: (d: DuplicateView) => triageApi.join(d.incidentId),
    onSuccess: (_, d) => {
      toast(`You're now following ${d.code}. We'll tell you when it's fixed, no new ticket needed.`, 'success')
      navigate(`/employee/incidents/${d.incidentId}`)
    },
    onError: (e) => toast(e instanceof Error ? e.message : 'Could not join the incident.', 'error'),
  })

  const submit = useMutation({
    mutationFn: () =>
      submitIncident({
        title: titleText.trim(),
        description: description.trim(),
        impact,
        urgency,
        affectedUsers,
        // Only an explicit choice is sent; otherwise the incident module classifies with triage itself.
        categoryCode: category || serviceName ? effectiveCategory || null : null,
        serviceId: category || serviceName
          ? options.data?.services.find((s) => s.name === effectiveService)?.id ?? null
          : null,
      }),
    onSuccess: (created) => {
      toast(created.assigneeId
        ? `Reported as ${created.code} and assigned to a technician.`
        : `Reported as ${created.code}. A team lead will assign it shortly.`, 'success')
      navigate(`/employee/incidents/${created.id}`)
    },
    onError: (e) => {
      if (e instanceof ApiError && (e.problem.status === 404 || e.problem.status === 405)) {
        toast('Incident submission is not connected yet (incident module).', 'error')
      } else {
        toast(e instanceof Error ? e.message : 'Could not submit the incident.', 'error')
      }
    },
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!titleText.trim() || !description.trim() || !effectiveCategory) {
      toast('Please add a title, a description and a category.', 'error')
      return
    }
    submit.mutate()
  }

  return (
    <>
      <PageHeader title="Report an issue" subtitle="Describe what is wrong. We check for an existing report while you type." />

      {duplicate && (
        <div className="mb-5 rounded-xl border border-amber-300 bg-amber-50 p-4" role="alert">
          <div className="flex flex-wrap items-start gap-3">
            <div className="flex-1 min-w-[16rem]">
              <div className="text-sm font-semibold text-amber-900">
                Looks like {duplicate.code} ({percent(duplicate.score)} match), reported {minutesAgo(duplicate.minutesAgo)}.
              </div>
              <div className="text-sm text-amber-900 mt-0.5">“{duplicate.title}”</div>
              <div className="text-xs text-amber-800 mt-1">
                Status {label(duplicate.status)}{duplicate.serviceName ? ` · ${duplicate.serviceName}` : ''}. Joining adds you as an
                affected user, which can raise its priority.
              </div>
            </div>
            <div className="flex gap-2">
              <button type="button" onClick={() => join.mutate(duplicate)} disabled={join.isPending}
                className="rounded-md bg-amber-600 text-white px-3 py-2 text-sm font-medium hover:bg-amber-700 disabled:opacity-60">
                {join.isPending ? 'Joining…' : `Yes, join ${duplicate.code}`}
              </button>
              <button type="button" onClick={() => setDismissed((d) => [...d, duplicate.code])}
                className="rounded-md bg-white ring-1 ring-amber-300 text-amber-900 px-3 py-2 text-sm hover:bg-amber-100">
                No, mine is different
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="grid lg:grid-cols-[1fr_20rem] gap-6">
        <form onSubmit={onSubmit} className="rounded-xl bg-white ring-1 ring-slate-200 p-5 space-y-4" noValidate>
          <label className="block">
            <span className="block text-sm font-medium text-slate-700 mb-1">What is not working?</span>
            <input value={titleText} onChange={(e) => setTitleText(e.target.value)} maxLength={200} required
              placeholder="e.g. Fee payment app unavailable"
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
          </label>
          <label className="block">
            <span className="flex items-center justify-between text-sm font-medium text-slate-700 mb-1">
              <span>Details</span>
              {speech.supported && (
                <button type="button" onClick={speech.toggle}
                  className={`text-xs rounded-full px-2.5 py-1 ${speech.listening ? 'bg-red-100 text-red-700' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'}`}
                  aria-pressed={speech.listening}>
                  {speech.listening ? '● Listening… tap to stop' : '🎤 Speak instead'}
                </button>
              )}
            </span>
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={5} maxLength={4000} required
              placeholder="What happened, since when, and who is affected?"
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" />
            {speech.error && <span className="block text-xs text-red-600 mt-1">{speech.error}</span>}
          </label>

          <div className="grid sm:grid-cols-3 gap-3">
            <label className="block">
              <span className="block text-sm font-medium text-slate-700 mb-1">Who is affected?</span>
              <select value={impact} onChange={(e) => setImpact(e.target.value as Impact)}
                className="w-full rounded-md border border-slate-300 px-2 py-2 text-sm">
                {IMPACTS.map((i) => <option key={i.value} value={i.value}>{i.text}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="block text-sm font-medium text-slate-700 mb-1">How urgent?</span>
              <select value={urgency} onChange={(e) => setUrgency(e.target.value as Urgency)}
                className="w-full rounded-md border border-slate-300 px-2 py-2 text-sm">
                {URGENCIES.map((u) => <option key={u.value} value={u.value}>{u.text}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="block text-sm font-medium text-slate-700 mb-1">People affected</span>
              <input type="number" min={1} max={100000} value={affectedUsers}
                onChange={(e) => setAffectedUsers(Math.max(1, Number(e.target.value) || 1))}
                className="w-full rounded-md border border-slate-300 px-2 py-2 text-sm" />
            </label>
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <label className="block">
              <span className="block text-sm font-medium text-slate-700 mb-1">
                Category {category === null && p && <span className="text-xs font-normal text-violet-700">· suggested</span>}
              </span>
              <select value={effectiveCategory} onChange={(e) => setCategory(e.target.value)}
                className="w-full rounded-md border border-slate-300 px-2 py-2 text-sm">
                <option value="" disabled>Choose…</option>
                {(options.data?.categories ?? []).map((c) => <option key={c} value={c}>{label(c)}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="block text-sm font-medium text-slate-700 mb-1">
                Service {serviceName === null && p?.suggestedServiceName && <span className="text-xs font-normal text-violet-700">· suggested</span>}
              </span>
              <select value={effectiveService} onChange={(e) => setServiceName(e.target.value)}
                className="w-full rounded-md border border-slate-300 px-2 py-2 text-sm">
                <option value="">Not sure</option>
                {(options.data?.services ?? []).map((s) => <option key={s.name} value={s.name}>{s.name}</option>)}
              </select>
            </label>
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <button type="submit" disabled={submit.isPending}
              className="rounded-md bg-blue-600 text-white px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-60">
              {submit.isPending ? 'Submitting…' : 'Submit report'}
            </button>
          </div>
        </form>

        <aside className="space-y-4">
          <div className="rounded-xl bg-white ring-1 ring-slate-200 p-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-slate-900">AI triage</h2>
              {preview.isFetching && <span className="text-xs text-slate-400">checking…</span>}
            </div>
            {!ready && <p className="text-sm text-slate-500 mt-2">Start typing and we'll suggest a category and look for existing reports.</p>}
            {ready && p && (
              <div className="mt-3 space-y-3">
                <div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-medium">{label(p.suggestion.categoryCode)}</span>
                    <span className="text-slate-500">{percent(p.suggestion.confidence)}</span>
                  </div>
                  <Meter value={p.suggestion.confidence} tone={p.lowConfidence ? 'bg-amber-500' : 'bg-violet-500'} />
                  {p.suggestedServiceName && <div className="text-xs text-slate-600 mt-1">Service: {p.suggestedServiceName}</div>}
                  <div className="text-xs text-slate-500 mt-1">{sourceLabel(p.suggestion.source)}: {p.suggestion.explanation}</div>
                </div>
                {p.votes.length > 0 && (
                  <button type="button" onClick={() => setShowWhy((v) => !v)} className="text-xs text-violet-700 hover:underline">
                    {showWhy ? 'Hide' : 'Why this suggestion?'}
                  </button>
                )}
                {showWhy && (
                  <ul className="space-y-2">
                    {p.votes.map((v) => (
                      <li key={v.source} className="text-xs">
                        <div className="flex justify-between"><span className="font-medium">{sourceLabel(v.source)}</span>
                          <span>{v.categoryCode ? `${label(v.categoryCode)} ${percent(v.confidence)}` : 'no category'}</span></div>
                        <div className="text-slate-500">{v.explanation}</div>
                      </li>
                    ))}
                  </ul>
                )}
                {p.possiblyRelated.length > 0 && (
                  <div>
                    <div className="text-xs font-semibold text-slate-700 mb-1">Possibly related</div>
                    <ul className="space-y-1">
                      {p.possiblyRelated.map((d) => (
                        <li key={d.code} className="text-xs text-slate-600 flex justify-between gap-2">
                          <span className="truncate">{d.code} · {d.title}</span>
                          <Pill>{percent(d.score)}</Pill>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                <div className="text-[11px] text-slate-400">Model: {p.embedder}</div>
              </div>
            )}
            {ready && preview.isError && <p className="text-xs text-red-600 mt-2">Suggestions are unavailable right now; you can still submit.</p>}
          </div>
        </aside>
      </div>
    </>
  )
}
