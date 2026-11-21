import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { PageHeader } from '../../app/PageHeader'
import { useToast } from '../../app/ui/Toasts'
import { useAuth } from '../auth/AuthContext'
import { TechnicianRecommendationPanel } from '../assignment/TechnicianRecommendationPanel'
import { triageApi } from './api'
import { ResolutionSuggestionPanel } from './ResolutionSuggestionPanel'
import { SimilarIncidentsPanel } from './SimilarIncidentsPanel'
import { Card } from './ui'

/**
 * Everything the intelligence layer knows about one incident, on one page. Member 1's incident detail page can
 * embed the same panels; until then technicians open this from their queue.
 */
export default function ResolutionAssistantPage() {
  const { incidentId } = useParams()
  const id = Number(incidentId)
  const { user } = useAuth()
  const toast = useToast()
  const [parent, setParent] = useState('')

  const merge = useMutation({
    mutationFn: () => triageApi.merge(id, Number(parent)),
    onSuccess: () => toast(`Merged into incident ${parent}. Its reporter now follows the parent.`, 'success'),
    onError: (e) => toast(e instanceof Error ? e.message : 'Merge failed.', 'error'),
  })

  function onMerge(e: FormEvent) {
    e.preventDefault()
    if (!Number.isInteger(Number(parent)) || Number(parent) <= 0) {
      toast('Enter the id of the incident to merge into.', 'error')
      return
    }
    merge.mutate()
  }

  if (!Number.isFinite(id) || id <= 0) {
    return <p className="text-sm text-slate-600">Unknown incident. <Link to="/technician/workload" className="text-teal-700 underline">Back to my queue</Link></p>
  }

  return (
    <>
      <PageHeader title={`Resolution assistant · incident ${id}`} subtitle="Similar past fixes, a suggested resolution and duplicate handling."
        actions={<Link to={`/technician/incidents/${id}`} className="text-sm text-teal-700 hover:underline">← Back to the incident</Link>} />
      <div className="grid xl:grid-cols-2 gap-5">
        <ResolutionSuggestionPanel incidentId={id} />
        <SimilarIncidentsPanel incidentId={id} />
        {user?.role === 'TEAM_LEAD' && <TechnicianRecommendationPanel incidentId={id} />}
        <Card title="Duplicate of another incident?" subtitle="Merging closes this one as a duplicate and keeps its reporter informed.">
          <form onSubmit={onMerge} className="flex gap-2">
            <input value={parent} onChange={(e) => setParent(e.target.value)} placeholder="Parent incident id"
              className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm" inputMode="numeric" />
            <button type="submit" disabled={merge.isPending}
              className="rounded-md bg-slate-900 text-white px-3 py-2 text-sm hover:bg-slate-800 disabled:opacity-60">
              {merge.isPending ? 'Merging…' : 'Merge'}
            </button>
          </form>
        </Card>
      </div>
    </>
  )
}
