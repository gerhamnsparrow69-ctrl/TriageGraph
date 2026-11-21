import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'

type Tone = 'error' | 'success' | 'info'
interface Toast { id: number; message: string; tone: Tone }

const ToastContext = createContext<(message: string, tone?: Tone) => void>(() => {})

const toneClass: Record<Tone, string> = {
  error: 'bg-red-600',
  success: 'bg-emerald-600',
  info: 'bg-slate-800',
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const show = useCallback((message: string, tone: Tone = 'info') => {
    const id = Date.now() + Math.random()
    setToasts((t) => [...t, { id, message, tone }])
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 5000)
  }, [])

  return (
    <ToastContext.Provider value={show}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 max-w-sm" role="status" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`${toneClass[t.tone]} text-white text-sm rounded-lg shadow-lg px-4 py-3`}>
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
