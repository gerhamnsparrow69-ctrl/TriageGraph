import WorkloadPage from '../features/assignment/WorkloadPage'
import { Link } from 'react-router'
import { useAuth } from '../features/auth/AuthContext'
import { MyIncidents } from '../features/search/MyIncidents'
import { PageHeader } from './PageHeader'

export function EmployeeHome() {
  const { user } = useAuth()
  return (
    <>
      <PageHeader
        title={`Hi ${user?.name?.split(' ')[0] ?? ''}`}
        subtitle="Your reported incidents and their status."
        actions={
          <Link to="/employee/report" className="rounded-md bg-blue-600 text-white px-4 py-2 text-sm font-medium hover:bg-blue-700">
            Report an issue
          </Link>
        }
      />
      <MyIncidents />
    </>
  )
}

/** The technician's queue is Member 2's workload page (assigned incidents, most urgent first). */
export function TechnicianHome() {
  return <WorkloadPage />
}
