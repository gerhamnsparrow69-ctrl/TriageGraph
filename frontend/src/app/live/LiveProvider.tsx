import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { post, type Schemas } from '../../api/client'
import { useAuth } from '../../features/auth/AuthContext'
import { LiveContext, type Listener, type LiveState } from './LiveContext'

const EVENTS = ['notification', 'feed']

/**
 * One EventSource per signed-in tab. EventSource can't send headers, so we fetch a short-lived
 * stream token first and reconnect with a fresh one (with backoff) whenever the stream drops.
 */
export function LiveProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const listeners = useRef(new Map<string, Set<Listener>>())
  const [connected, setConnected] = useState(false)

  const subscribe = useCallback((event: string, listener: Listener) => {
    const set = listeners.current.get(event) ?? new Set<Listener>()
    set.add(listener)
    listeners.current.set(event, set)
    return () => {
      set.delete(listener)
    }
  }, [])

  useEffect(() => {
    if (!user) return
    let source: EventSource | null = null
    let retry: ReturnType<typeof setTimeout> | undefined
    let delay = 2000
    let stopped = false

    async function open() {
      try {
        const { token } = await post<Schemas['TokenResponse']>('/auth/stream-token')
        if (stopped || !token) return
        source = new EventSource(`/api/v1/stream?access_token=${encodeURIComponent(token)}`)
        source.addEventListener('ready', () => {
          setConnected(true)
          delay = 2000
        })
        for (const name of EVENTS) {
          source.addEventListener(name, (e) => {
            const data = JSON.parse((e as MessageEvent).data)
            listeners.current.get(name)?.forEach((l) => l(data))
          })
        }
        source.onerror = () => {
          source?.close()
          setConnected(false)
          schedule()
        }
      } catch {
        schedule()
      }
    }

    function schedule() {
      if (stopped) return
      retry = setTimeout(open, delay)
      delay = Math.min(delay * 2, 30000)
    }

    open()
    return () => {
      stopped = true
      clearTimeout(retry)
      source?.close()
      setConnected(false)
    }
  }, [user])

  const value = useMemo<LiveState>(() => ({ connected, subscribe }), [connected, subscribe])
  return <LiveContext.Provider value={value}>{children}</LiveContext.Provider>
}
