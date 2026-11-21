import { useAuth } from '../auth/AuthContext'
import { useIncidentAction } from '../incident/api'
import type { IncidentPanelProps } from '../incident/slots'
import { useToast } from '../../app/ui/Toasts'
import { TechnicianRecommendationPanel } from './TechnicianRecommendationPanel'

const ASSIGNABLE = ['CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED']

/**
 * Member 2's side panel on the incident page: ranked technicians with reasons (Feature D). Shown to team
 * leads and admins while the incident can still be (re)assigned; picking one uses the incident module's
 * own assign action, so the lifecycle rules and audit stay in one place.
 */
export default function IncidentAssignmentPanel({ incident }: IncidentPanelProps) {
  const { user } = useAuth()
  const action = useIncidentAction(incident.id)
  const toast = useToast()
  const canAssign = user?.role === 'TEAM_LEAD' || user?.role === 'ADMIN'
  if (!canAssign || !ASSIGNABLE.includes(incident.status)) return null
  const pick = (technicianId: number) =>
    action.mutate({ path: 'assign', body: { technicianId } }, {
      onSuccess: () => toast(`${incident.code} reassigned.`, 'success'),
      onError: (e) => toast(e instanceof Error ? e.message : 'Could not assign.', 'error'),
    })
  return <TechnicianRecommendationPanel incidentId={incident.id} onPick={pick} />
}
