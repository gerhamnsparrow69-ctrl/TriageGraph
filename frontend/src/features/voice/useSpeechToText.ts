import { useCallback, useEffect, useRef, useState } from 'react'
import { errorMessage, finalText, recognitionConstructor, type Recognition } from './speech'

/** Wraps the browser's speech recognition: start/stop, listening state and friendly errors. */
export function useSpeechToText(onText: (text: string) => void, lang = 'en-IN') {
  const Ctor = recognitionConstructor()
  const recognition = useRef<Recognition | null>(null)
  const [listening, setListening] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const latest = useRef(onText)

  useEffect(() => {
    latest.current = onText
  }, [onText])

  useEffect(() => () => recognition.current?.abort(), [])

  const start = useCallback(() => {
    if (!Ctor || recognition.current) return
    const r = new Ctor()
    r.lang = lang
    r.continuous = false
    r.interimResults = false
    r.onresult = (e) => {
      const text = finalText(e)
      if (text) latest.current(text)
    }
    r.onerror = (e) => {
      if (e.error !== 'aborted') setError(errorMessage(e.error))
    }
    r.onend = () => {
      recognition.current = null
      setListening(false)
    }
    recognition.current = r
    setError(null)
    setListening(true)
    r.start()
  }, [Ctor, lang])

  const stop = useCallback(() => recognition.current?.stop(), [])

  return { supported: Ctor !== null, listening, error, start, stop }
}
