import { useState } from 'react'
import { emptyFilters, useIncidentSearch } from './api'
import { IncidentTable, Pager } from './IncidentTable'

const PAGE_SIZE = 10

/** The employee home list: incidents you reported or joined, newest first. */
export function MyIncidents() {
  const [page, setPage] = useState(0)
  const [openOnly, setOpenOnly] = useState(false)
  const filters = openOnly
    ? { ...emptyFilters, status: ['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'PENDING_INFO', 'RESOLVED'] }
    : emptyFilters
  const result = useIncidentSearch(filters, page, PAGE_SIZE)

  if (result.isPending) return <p className="text-sm text-slate-500">Loading your incidents…</p>
  if (result.isError) return <p className="text-sm text-red-700">{result.error.message}</p>
  if (result.data.total === 0 && !openOnly) {
    return (
      <div className="rounded-xl bg-white ring-1 ring-slate-200 p-8 text-center text-slate-500 text-sm">
        You haven't reported anything yet.
      </div>
    )
  }
  return (
    <>
      <label className="flex items-center gap-2 text-sm text-slate-600 mb-3">
        <input type="checkbox" checked={openOnly} onChange={(e) => { setOpenOnly(e.target.checked); setPage(0) }} />
        Only ones that still need something
      </label>
      {result.data.total === 0 ? (
        <div className="rounded-xl bg-white ring-1 ring-slate-200 p-8 text-center text-slate-500 text-sm">Nothing open right now.</div>
      ) : (
        <>
          <IncidentTable rows={result.data.items} basePath="/employee" showPeople={false} />
          <Pager page={page} size={PAGE_SIZE} total={result.data.total} onPage={setPage} />
        </>
      )}
    </>
  )
}
