import type { RouteObject } from 'react-router'
import TeamWorkloadPage from './TeamWorkloadPage'
import WorkloadPage from './WorkloadPage'

/** /technician/workload and /technician/team */
export const assignmentTechnicianRoutes: RouteObject[] = [
  { path: 'workload', element: <WorkloadPage /> },
  { path: 'team', element: <TeamWorkloadPage /> },
]
