import type { components } from './schema'

export type Schemas = components['schemas']
export type Role = NonNullable<Schemas['UserResponse']['role']>
export type User = Schemas['UserResponse']

const TOKEN_KEY = 'tg.token'

export const tokenStore = {
  get: (): string | null => {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set: (token: string | null) => {
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token)
      else localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* private mode: keep the session in memory only */
    }
  },
}

/** RFC 9457 ProblemDetail as returned by the backend's exception handlers. */
export interface Problem {
  status: number
  title?: string
  detail?: string
  errorCode?: string
  fieldErrors?: { field: string; message: string }[]
  lockedUntil?: string
}

export class ApiError extends Error {
  readonly problem: Problem

  constructor(problem: Problem) {
    super(problem.detail ?? problem.title ?? `Request failed (${problem.status})`)
    this.problem = problem
  }
}

/** Called when any request comes back 401, so the app can drop the session. */
let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const token = tokenStore.get()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  const res = await fetch(`/api/v1${path}`, { ...init, headers })
  if (res.status === 204) return undefined as T

  const text = await res.text()
  const body = text ? JSON.parse(text) : undefined
  if (!res.ok) {
    if (res.status === 401 && token) onUnauthorized()
    throw new ApiError({ status: res.status, ...(body ?? {}) })
  }
  return body as T
}

export const post = <T>(path: string, body?: unknown) =>
  api<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })

export const put = <T>(path: string, body: unknown) =>
  api<T>(path, { method: 'PUT', body: JSON.stringify(body) })
