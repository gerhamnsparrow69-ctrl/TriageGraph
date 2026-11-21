import { Link } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import type { IncidentPanelProps } from '../incident/slots'
import { SimilarIncidentsPanel } from './SimilarIncidentsPanel'

const CLOSED = ['CLOSED', 'MERGED']

/**
 * Member 2's side panel on the incident page: how similar incidents were fixed, plus a link to the
 * resolution assistant for the people who work incidents. Employees see nothing (the endpoints are staff-only).
 */
export default function IncidentTriagePanel({ incident }: IncidentPanelProps) {
  const { user } = useAuth()
  if (!user || user.role === 'EMPLOYEE') return null
  const worksIncidents = user.role === 'TECHNICIAN' || user.role === 'TEAM_LEAD'
  return (
    <div className="space-y-2">
      {worksIncidents && !CLOSED.includes(incident.status) && !incident.resolution && (
        <Link to={`/technician/assist/${incident.id}`}
          className="block rounded-lg bg-violet-600 px-3 py-2 text-center text-sm font-medium text-white hover:bg-violet-700">
          Open resolution assistant
        </Link>
      )}
      <SimilarIncidentsPanel incidentId={incident.id} />
    </div>
  )
}
