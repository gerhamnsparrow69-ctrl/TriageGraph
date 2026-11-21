import { ApiError, type Role } from '../../api/client'

export const roleLabel: Record<Role, string> = {
  EMPLOYEE: 'Employee',
  TECHNICIAN: 'Technician',
  TEAM_LEAD: 'Team lead',
  ADMIN: 'Administrator',
  IT_MANAGER: 'IT manager',
}

/** Splits a failed request into per-field messages and one general message. */
export function readProblem(err: unknown): { fields: Record<string, string>; message: string | null } {
  if (err instanceof ApiError) {
    const fields = Object.fromEntries((err.problem.fieldErrors ?? []).map((f) => [f.field, f.message]))
    return { fields, message: Object.keys(fields).length ? null : err.message }
  }
  return { fields: {}, message: err instanceof Error ? err.message : 'Something went wrong.' }
}

export const primaryButton =
  'rounded-md bg-indigo-600 text-white px-4 py-2 text-sm font-medium hover:bg-indigo-700 disabled:opacity-50'
export const secondaryButton =
  'rounded-md bg-white ring-1 ring-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50 disabled:opacity-50'
