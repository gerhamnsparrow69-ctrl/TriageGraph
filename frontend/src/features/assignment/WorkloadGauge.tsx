import type { TechnicianWorkload } from './api'

/** Load as a coloured bar: green under 60 %, amber under 100 %, red when full. */
export function WorkloadGauge({ w, compact = false }: { w: TechnicianWorkload; compact?: boolean }) {
  const tone = w.atCapacity ? 'bg-red-500' : w.loadPercent >= 60 ? 'bg-amber-500' : 'bg-emerald-500'
  return (
    <div>
      <div className={`flex items-baseline justify-between ${compact ? 'text-xs' : 'text-sm'}`}>
        <span className="text-slate-600">{w.activeLoad} of {w.maxLoad} slots in use</span>
        <span className="font-semibold">{w.loadPercent}%</span>
      </div>
      <div className={`${compact ? 'h-1.5' : 'h-2.5'} rounded-full bg-slate-100 mt-1`}>
        <div className={`${compact ? 'h-1.5' : 'h-2.5'} rounded-full ${tone}`} style={{ width: `${Math.min(100, w.loadPercent)}%` }} />
      </div>
      {!w.available && <div className="text-xs text-amber-700 mt-1">Marked unavailable: no new automatic assignments</div>}
    </div>
  )
}
