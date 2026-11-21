import { useLocation } from 'react-router'
import { PageHeader } from './PageHeader'

/** Fallback inside each layout for pages that a teammate hasn't built yet. */
export default function ComingSoon() {
  const { pathname } = useLocation()
  return (
    <>
      <PageHeader title="Not built yet" subtitle={pathname} />
      <div className="rounded-xl border border-dashed border-slate-300 bg-white p-8 text-slate-500 text-sm">
        This page is on the plan but hasn't been built yet.
      </div>
    </>
  )
}
