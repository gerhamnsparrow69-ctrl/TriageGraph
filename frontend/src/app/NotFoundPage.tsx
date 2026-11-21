import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <main className="min-h-screen flex flex-col items-center justify-center gap-3 p-6">
      <h1 className="text-2xl font-semibold">404: page not found</h1>
      <Link to="/" className="text-blue-600 underline">Back to home</Link>
    </main>
  )
}
