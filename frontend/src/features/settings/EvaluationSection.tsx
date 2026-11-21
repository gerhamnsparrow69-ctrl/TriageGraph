import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { CartesianGrid, Legend, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { triageApi } from '../triage/api'
import { Card, ErrorNote, Pill, Spinner } from '../triage/ui'
import { percent } from '../triage/format'

/** How good duplicate detection really is, measured on 30 labelled pairs (the "Evaluate" step). */
export function EvaluationSection() {
  const q = useQuery({ queryKey: ['triage-evaluation'], queryFn: triageApi.evaluation, staleTime: 60_000 })
  const [showPairs, setShowPairs] = useState(false)
  return (
    <Card title="Measured accuracy" subtitle="Duplicate detection on 30 labelled incident pairs (12 true duplicates)">
      {q.isPending && <Spinner label="Scoring the labelled pairs…" />}
      {q.isError && <ErrorNote error={q.error} />}
      {q.data && (
        <>
          <div className="grid grid-cols-3 gap-3 mb-4">
            <Tile label="Precision" value={percent(q.data.atThreshold.precision)} />
            <Tile label="Recall" value={percent(q.data.atThreshold.recall)} />
            <Tile label="F1" value={q.data.atThreshold.f1.toFixed(2)} />
          </div>
          <p className="text-xs text-slate-600 mb-2">
            At threshold {q.data.atThreshold.threshold.toFixed(2)}: {q.data.atThreshold.tp} found, {q.data.atThreshold.fp} false alarm(s),
            {' '}{q.data.atThreshold.fn} missed. Best F1 {q.data.best.f1.toFixed(2)} at {q.data.best.threshold.toFixed(2)}. Model: {q.data.embedder}.
          </p>
          <div className="h-56">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={q.data.sweep} margin={{ top: 8, right: 8, bottom: 0, left: -20 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="threshold" tick={{ fontSize: 11 }} />
                <YAxis domain={[0, 1]} tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v) => (typeof v === 'number' ? v.toFixed(2) : String(v))} />
                <Legend wrapperStyle={{ fontSize: 12 }} />
                <ReferenceLine x={q.data.atThreshold.threshold} stroke="#4f46e5" strokeDasharray="4 4" />
                <Line type="monotone" dataKey="f1" stroke="#4f46e5" dot={false} name="F1" />
                <Line type="monotone" dataKey="precision" stroke="#059669" dot={false} name="Precision" />
                <Line type="monotone" dataKey="recall" stroke="#d97706" dot={false} name="Recall" />
              </LineChart>
            </ResponsiveContainer>
          </div>
          <button type="button" onClick={() => setShowPairs((v) => !v)} className="text-xs text-indigo-700 hover:underline mt-2">
            {showPairs ? 'Hide the pairs' : 'Show every pair'}
          </button>
          {showPairs && (
            <table className="w-full text-xs mt-2">
              <thead><tr className="text-left text-slate-500"><th className="py-1">Score</th><th>Truth</th><th>Predicted</th><th>Pair</th></tr></thead>
              <tbody>
                {q.data.results.map((r, i) => (
                  <tr key={i} className="border-t border-slate-100 align-top">
                    <td className="py-1 pr-2 font-mono">{r.score.toFixed(2)}</td>
                    <td className="pr-2">{r.duplicate ? <Pill tone="green">duplicate</Pill> : <Pill>different</Pill>}</td>
                    <td className="pr-2">{r.predictedDuplicate === r.duplicate ? '✓' : <span className="text-red-600">✗</span>}</td>
                    <td className="text-slate-600">{r.textA} <span className="text-slate-400">⇄</span> {r.textB}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </Card>
  )
}

function Tile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-slate-50 p-3">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className="text-xl font-semibold text-slate-900">{value}</div>
    </div>
  )
}
