import { Link } from 'react-router'
import { homeFor, useAuth } from '../features/auth/AuthContext'

function ErrorPage({ code, title, message }: { code: string; title: string; message: string }) {
  const { user } = useAuth()
  return (
    <main className="min-h-screen grid place-items-center bg-slate-50 px-4">
      <div className="text-center max-w-sm">
        <div className="text-6xl font-bold text-slate-300">{code}</div>
        <h1 className="text-xl font-semibold text-slate-900 mt-2">{title}</h1>
        <p className="text-slate-500 text-sm mt-2">{message}</p>
        <Link to={user ? homeFor(user.role) : '/login'} className="inline-block mt-6 text-blue-600 hover:underline text-sm">
          {user ? 'Back to your home page' : 'Go to sign in'}
        </Link>
      </div>
    </main>
  )
}

export const ForbiddenPage = () => (
  <ErrorPage code="403" title="Not allowed" message="Your role doesn't have access to this page." />
)

export const NotFoundPage = () => (
  <ErrorPage code="404" title="Page not found" message="That address doesn't match any page." />
)
