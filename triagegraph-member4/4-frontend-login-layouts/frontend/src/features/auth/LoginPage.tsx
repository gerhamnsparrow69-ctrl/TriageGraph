import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { ApiError } from '../../api/client'
import { AuthCard, Field } from './AuthCard'
import { homeFor, useAuth } from './AuthContext'

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={homeFor(user.role)} replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const u = await login(email, password)
      const from = (location.state as { from?: string } | null)?.from
      navigate(from ?? homeFor(u.role), { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.problem.status === 423 && err.problem.lockedUntil) {
        const until = new Date(err.problem.lockedUntil).toLocaleTimeString()
        setError(`Too many failed attempts. Try again after ${until}.`)
      } else {
        setError(err instanceof Error ? err.message : 'Login failed.')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthCard title="Sign in" subtitle="Use your campus account.">
      <form onSubmit={submit} noValidate>
        <Field label="Email" type="email" autoComplete="username" required value={email}
          onChange={(e) => setEmail(e.target.value)} />
        <Field label="Password" type="password" autoComplete="current-password" required value={password}
          onChange={(e) => setPassword(e.target.value)} />
        {error && <p className="text-sm text-red-600 mb-4" role="alert">{error}</p>}
        <button type="submit" disabled={busy}
          className="w-full rounded-md bg-slate-900 text-white py-2 text-sm font-medium hover:bg-slate-800 disabled:opacity-60">
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="text-sm text-slate-500 mt-4 text-center">
        New here? <Link to="/register" className="text-blue-600 hover:underline">Create an account</Link>
      </p>
    </AuthCard>
  )
}
