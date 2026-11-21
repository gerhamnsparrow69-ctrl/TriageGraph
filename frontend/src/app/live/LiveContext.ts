import { createContext, useContext, useEffect } from 'react'

export type Listener = (data: unknown) => void

export interface LiveState {
  connected: boolean
  subscribe: (event: string, listener: Listener) => () => void
}

export const LiveContext = createContext<LiveState>({ connected: false, subscribe: () => () => {} })

/** Run `listener` for every server-sent event with this name while the component is mounted. */
export function useLiveEvent<T>(event: string, listener: (data: T) => void) {
  const { subscribe } = useContext(LiveContext)
  useEffect(() => subscribe(event, (d) => listener(d as T)), [event, listener, subscribe])
}

export const useLiveStatus = () => useContext(LiveContext).connected
