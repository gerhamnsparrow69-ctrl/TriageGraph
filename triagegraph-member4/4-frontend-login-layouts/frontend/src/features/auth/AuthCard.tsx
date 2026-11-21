import type { ReactNode } from 'react'

export function AuthCard({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <main className="min-h-screen bg-gradient-to-br from-slate-100 to-slate-200 grid place-items-center px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="text-center mb-6">
          <div className="text-2xl font-bold tracking-tight text-slate-900">TriageGraph</div>
          <div className="text-sm text-slate-500">Campus IT service desk</div>
        </div>
        <div className="bg-white rounded-xl shadow-sm ring-1 ring-slate-200 p-6">
          <h1 className="text-lg font-semibold text-slate-900">{title}</h1>
          <p className="text-sm text-slate-500 mb-5">{subtitle}</p>
          {children}
        </div>
      </div>
    </main>
  )
}

export function Field({ label, error, ...input }: { label: string; error?: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="block mb-4">
      <span className="block text-sm font-medium text-slate-700 mb-1">{label}</span>
      <input
        {...input}
        className={`w-full rounded-md border px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500 ${
          error ? 'border-red-400' : 'border-slate-300'
        }`}
      />
      {error && <span className="block text-xs text-red-600 mt-1">{error}</span>}
    </label>
  )
}
