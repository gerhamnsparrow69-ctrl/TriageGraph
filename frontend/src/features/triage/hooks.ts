import { useEffect, useRef, useState } from 'react'

/** The value, but only after it has stopped changing for `delay` ms (live preview while typing). */
export function useDebounced<T>(value: T, delay = 600): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(t)
  }, [value, delay])
  return debounced
}

interface RecognitionResult {
  isFinal: boolean
  0: { transcript: string }
}

interface RecognitionEvent {
  resultIndex: number
  results: ArrayLike<RecognitionResult>
}

interface Recognition {
  lang: string
  interimResults: boolean
  continuous: boolean
  start(): void
  stop(): void
  onresult: ((e: RecognitionEvent) => void) | null
  onend: (() => void) | null
  onerror: ((e: { error: string }) => void) | null
}

type RecognitionCtor = new () => Recognition

function recognitionCtor(): RecognitionCtor | undefined {
  const w = window as unknown as { SpeechRecognition?: RecognitionCtor; webkitSpeechRecognition?: RecognitionCtor }
  return w.SpeechRecognition ?? w.webkitSpeechRecognition
}

/**
 * Voice input with the browser's Web Speech API (works in Chrome and Edge; on other browsers the mic button
 * is hidden). Final phrases are passed to `onText`.
 */
export function useSpeechInput(onText: (text: string) => void) {
  const [listening, setListening] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const ref = useRef<Recognition | null>(null)
  const supported = typeof window !== 'undefined' && recognitionCtor() !== undefined

  useEffect(() => () => ref.current?.stop(), [])

  function toggle() {
    if (listening) {
      ref.current?.stop()
      return
    }
    const Ctor = recognitionCtor()
    if (!Ctor) return
    const r = new Ctor()
    r.lang = 'en-IN'
    r.interimResults = false
    r.continuous = true
    r.onresult = (e) => {
      for (let i = e.resultIndex; i < e.results.length; i++) {
        const res = e.results[i]
        if (res.isFinal) onText(res[0].transcript.trim())
      }
    }
    r.onerror = (e) => setError(e.error === 'not-allowed' ? 'Microphone permission was denied.' : `Voice input stopped (${e.error}).`)
    r.onend = () => setListening(false)
    ref.current = r
    setError(null)
    r.start()
    setListening(true)
  }

  return { supported, listening, error, toggle }
}
