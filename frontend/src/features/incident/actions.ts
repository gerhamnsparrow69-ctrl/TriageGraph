import type { IncidentAction } from './types'

/** What the reporter or technician has to type before an action can be sent. */
export type ActionInput =
  | { kind: 'none' }
  | { kind: 'text'; prompt: string; placeholder: string }
  | { kind: 'resolve' }
  | { kind: 'technician' }
  | { kind: 'feedback' }

export interface ActionSpec {
  action: IncidentAction
  label: string
  /** Path under /incidents/{id}. */
  path: string
  input: ActionInput
  primary?: boolean
}

/**
 * The lifecycle buttons, in the order they appear. Which ones show is decided by the backend's
 * allowedActions for the current user, never by the UI: there is no status logic here.
 * CLASSIFY, MERGE and ESCALATE belong to other screens; ADD_NOTE has its own form.
 */
export const ACTION_SPECS: ActionSpec[] = [
  { action: 'ACKNOWLEDGE', label: 'Acknowledge', path: 'acknowledge', input: { kind: 'none' }, primary: true },
  { action: 'START_WORK', label: 'Start work', path: 'start', input: { kind: 'none' }, primary: true },
  { action: 'RESOLVE', label: 'Resolve', path: 'resolve', input: { kind: 'resolve' }, primary: true },
  {
    action: 'REQUEST_INFO', label: 'Ask the reporter', path: 'request-info',
    input: { kind: 'text', prompt: 'Question for the reporter', placeholder: 'Which browser are you using?' },
  },
  {
    action: 'PROVIDE_INFO', label: 'Reply to technician', path: 'provide-info', primary: true,
    input: { kind: 'text', prompt: 'Your answer', placeholder: 'Chrome on Windows' },
  },
  { action: 'VERIFY', label: 'Yes, it is fixed', path: 'verify', input: { kind: 'none' }, primary: true },
  {
    action: 'REOPEN', label: 'Still broken: reopen', path: 'reopen',
    input: { kind: 'text', prompt: 'What is still wrong?', placeholder: 'Payment still fails on mobile' },
  },
  { action: 'CLOSE', label: 'Close incident', path: 'close', input: { kind: 'none' }, primary: true },
  { action: 'ASSIGN', label: 'Reassign', path: 'assign', input: { kind: 'technician' } },
  { action: 'FEEDBACK', label: 'Rate the fix', path: 'feedback', input: { kind: 'feedback' }, primary: true },
]

export function actionsFor(allowed: readonly IncidentAction[]): ActionSpec[] {
  return ACTION_SPECS.filter((s) => allowed.includes(s.action))
}

/** Request body for an action, from what the user typed. */
export function bodyFor(spec: ActionSpec, values: Record<string, string>): unknown {
  switch (spec.input.kind) {
    case 'none':
      return undefined
    case 'text':
      return { text: values.text?.trim() }
    case 'resolve':
      return { summary: values.summary?.trim(), rootCause: values.rootCause?.trim() || undefined }
    case 'technician':
      return { technicianId: Number(values.technicianId) }
    case 'feedback':
      return { rating: Number(values.rating), comment: values.comment?.trim() || undefined }
  }
}

/** Whether the form has what the backend requires, so the Send button can be disabled. */
export function isComplete(spec: ActionSpec, values: Record<string, string>): boolean {
  switch (spec.input.kind) {
    case 'none':
      return true
    case 'text':
      return !!values.text?.trim()
    case 'resolve':
      return !!values.summary?.trim()
    case 'technician':
      return Number(values.technicianId) > 0
    case 'feedback':
      return Number(values.rating) >= 1 && Number(values.rating) <= 5
  }
}
