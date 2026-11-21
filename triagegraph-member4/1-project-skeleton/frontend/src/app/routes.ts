import type { RouteObject } from 'react-router'

/**
 * Each feature exports its own `routes` array from `src/features/<feature>/routes.tsx`
 * and adds one import + one spread line here. Nobody edits anyone else's lines (blueprint §22, rule 3).
 */
export const featureRoutes: RouteObject[] = [
  // ...authRoutes,        (M4)
  // ...incidentRoutes,    (M1)
  // ...triageRoutes,      (M2)
  // ...topologyRoutes,    (M3)
]
