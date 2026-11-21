/** Times are stored in UTC and always shown in IST (blueprint §15). */
const IST = new Intl.DateTimeFormat('en-IN', {
  timeZone: 'Asia/Kolkata',
  day: '2-digit',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false,
})

export function formatIst(iso: string | null | undefined): string {
  return iso ? `${IST.format(new Date(iso))} IST` : '—'
}

/** "in 12 min" / "3 min ago" relative to now. */
export function relative(iso: string, now: number = Date.now()): string {
  const diff = Math.round((new Date(iso).getTime() - now) / 60000)
  const abs = Math.abs(diff)
  const text = abs < 60 ? `${abs} min` : abs < 1440 ? `${Math.floor(abs / 60)} h ${abs % 60} min` : `${Math.floor(abs / 1440)} d`
  return diff >= 0 ? `in ${text}` : `${text} ago`
}

export function humanise(code: string): string {
  const s = code.toLowerCase().replace(/_/g, ' ')
  return s.charAt(0).toUpperCase() + s.slice(1)
}

export const statusClass: Record<string, string> = {
  NEW: 'bg-slate-100 text-slate-700',
  CLASSIFIED: 'bg-sky-100 text-sky-800',
  ASSIGNED: 'bg-indigo-100 text-indigo-800',
  ACKNOWLEDGED: 'bg-violet-100 text-violet-800',
  IN_PROGRESS: 'bg-amber-100 text-amber-800',
  PENDING_INFO: 'bg-orange-100 text-orange-800',
  RESOLVED: 'bg-emerald-100 text-emerald-800',
  VERIFIED: 'bg-teal-100 text-teal-800',
  CLOSED: 'bg-slate-200 text-slate-700',
  MERGED: 'bg-slate-200 text-slate-500',
}

export const priorityClass: Record<string, string> = {
  P1: 'bg-red-600 text-white',
  P2: 'bg-orange-500 text-white',
  P3: 'bg-yellow-400 text-yellow-950',
  P4: 'bg-slate-400 text-white',
}
