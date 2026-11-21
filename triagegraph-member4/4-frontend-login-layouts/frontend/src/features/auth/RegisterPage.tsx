import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError, post } from '../../api/client'
import { useToast } from '../../app/ui/Toasts'
import { AuthCard, Field } from './AuthCard'
import { useAuth } from './AuthContext'

export default function RegisterPage() {
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', department: '' })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setFieldErrors({})
    try {
      await post('/auth/register', { ...form, department: form.department || undefined })
      await login(form.email, form.password)
      toast('Welcome to TriageGraph!', 'success')
      navigate('/employee', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.problem.fieldErrors) {
        setFieldErrors(Object.fromEntries(err.problem.fieldErrors.map((f) => [f.field, f.message])))
      } else {
        setError(err instanceof Error ? err.message : 'Registration failed.')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthCard title="Create an account" subtitle="For students and staff reporting IT issues.">
      <form onSubmit={submit} noValidate>
        <Field label="Full name" required value={form.name} onChange={set('name')} error={fieldErrors.name} />
        <Field label="Email" type="email" required value={form.email} onChange={set('email')} error={fieldErrors.email} />
        <Field label="Password" type="password" autoComplete="new-password" required value={form.password}
          onChange={set('password')} error={fieldErrors.password} />
        <p className="text-xs text-slate-500 -mt-3 mb-4">At least 8 characters, with a letter and a digit.</p>
        <Field label="Department or hostel (optional)" value={form.department} onChange={set('department')} />
        {error && <p className="text-sm text-red-600 mb-4" role="alert">{error}</p>}
        <button type="submit" disabled={busy}
          className="w-full rounded-md bg-blue-600 text-white py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-60">
          {busy ? 'Creating…' : 'Create account'}
        </button>
      </form>
      <p className="text-sm text-slate-500 mt-4 text-center">
        Already registered? <Link to="/login" className="text-blue-600 hover:underline">Sign in</Link>
      </p>
    </AuthCard>
  )
}
