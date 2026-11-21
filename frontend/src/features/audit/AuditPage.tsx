import { useState } from 'react'
import { Link } from 'react-router'
import { PageHeader } from '../../app/PageHeader'
import { formatIst, humanise } from '../incident/format'
import { AUDIT_PAGE_SIZE, useAuditPage, useVerifyAudit } from './api'
import { actorLabel, shortHash, transitionLabel } from './describe'

/** /admin/audit: the whole trail, newest first, and the integrity check. (MVP: no filter boxes.) */
export default function AuditPage() {
  const [page, setPage] = useState(0)
  const { data, isPending, isError, isPlaceholderData } = useAuditPage(page)
  const verify = useVerifyAudit()
  const result = verify.data

  return (
    <>
      <PageHeader
        title="Audit trail"
        subtitle="Every change, chained with SHA-256 so any edited or deleted row is detected."
        actions={
          <button onClick={() => verify.mutate()} disabled={verify.isPending}
            className="rounded-md bg-indigo-600 text-white px-4 py-2 text-sm font-medium hover:bg-indigo-700 disabled:opacity-50">
            {verify.isPending ? 'Verifying…' : 'Verify integrity'}
          </button>
        }
      />

      {result && (
        <div role="status" className={`mb-4 rounded-lg p-3 text-sm ring-1 ${result.valid
          ? 'bg-emerald-50 text-emerald-800 ring-emerald-200' : 'bg-red-50 text-red-800 ring-red-200'}`}>
          <span className="font-semibold">{result.valid ? 'Chain intact. ' : 'Tampering detected. '}</span>
          {result.message}
        </div>
      )}
      {verify.isError && <p className="mb-4 text-sm text-red-600">Verification could not run.</p>}

      {isPending && <p className="text-sm text-slate-500">Loading…</p>}
      {isError && <p className="text-sm text-red-600">Could not load the audit trail.</p>}
      {data && (
        <div className="overflow-x-auto rounded-xl ring-1 ring-slate-200 bg-white">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-3 py-2">#</th>
                <th className="px-3 py-2">When (IST)</th>
                <th className="px-3 py-2">Incident</th>
                <th className="px-3 py-2">Action</th>
                <th className="px-3 py-2">State</th>
                <th className="px-3 py-2">Actor</th>
                <th className="px-3 py-2">Description</th>
                <th className="px-3 py-2">Hash</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data.length === 0 && (
                <tr><td colSpan={8} className="px-3 py-6 text-center text-slate-500">No entries.</td></tr>
              )}
              {data.map((e) => (
                <tr key={e.id} className={result && !result.valid && result.firstBrokenId === e.id ? 'bg-red-50' : undefined}>
                  <td className="px-3 py-2 font-mono text-slate-500">{e.id}</td>
                  <td className="px-3 py-2 whitespace-nowrap">{formatIst(e.occurredAt).replace(' IST', '')}</td>
                  <td className="px-3 py-2 whitespace-nowrap">
                    {e.incidentId ? <Link className="text-indigo-700 hover:underline" to={`/admin/incidents/${e.incidentId}`}>{e.incidentCode}</Link> : '—'}
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap font-medium">{humanise(e.action)}</td>
                  <td className="px-3 py-2 whitespace-nowrap text-slate-500">{transitionLabel(e) ?? ''}</td>
                  <td className="px-3 py-2 whitespace-nowrap">{actorLabel(e)}</td>
                  <td className="px-3 py-2 text-slate-600 max-w-md truncate" title={e.description ?? undefined}>{e.description}</td>
                  <td className="px-3 py-2 font-mono text-xs text-slate-400" title={e.hash}>{shortHash(e.hash)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="mt-4 flex items-center gap-2 text-sm">
        <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}
          className="rounded-md bg-white ring-1 ring-slate-300 px-3 py-1.5 disabled:opacity-50">Newer</button>
        <span className="text-slate-500">Page {page + 1}</span>
        <button onClick={() => setPage((p) => p + 1)} disabled={!data || data.length < AUDIT_PAGE_SIZE || isPlaceholderData}
          className="rounded-md bg-white ring-1 ring-slate-300 px-3 py-1.5 disabled:opacity-50">Older</button>
      </div>
    </>
  )
}
