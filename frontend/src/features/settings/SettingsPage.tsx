import { PageHeader } from '../../app/PageHeader'
import { useAuth } from '../auth/AuthContext'
import { AssignmentSettingsSection } from './AssignmentSettingsSection'
import { EvaluationSection } from './EvaluationSection'
import { KeywordsSection } from './KeywordsSection'
import { TriageSettingsSection } from './TriageSettingsSection'

/** Admin settings for the intelligence layer. The IT manager can view everything; only admins change it. */
export default function SettingsPage() {
  const { user } = useAuth()
  const canEdit = user?.role === 'ADMIN'
  return (
    <>
      <PageHeader title="Settings" subtitle={canEdit ? 'Assignment strategy, triage and AI.' : 'Read-only: only administrators can change settings.'} />
      <div className="grid 2xl:grid-cols-2 gap-5">
        <AssignmentSettingsSection canEdit={canEdit} />
        <TriageSettingsSection canEdit={canEdit} />
        <EvaluationSection />
        <KeywordsSection canEdit={canEdit} />
      </div>
    </>
  )
}
