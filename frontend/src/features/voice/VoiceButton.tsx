import { useSpeechToText } from './useSpeechToText'

interface Props {
  /** Called with each finished phrase; the caller decides whether to append or replace. */
  onText: (text: string) => void
  lang?: string
  className?: string
}

/**
 * Microphone button for dictating an incident description or a search.
 * Renders nothing in browsers without speech recognition (e.g. Firefox), so forms still work.
 */
export function VoiceButton({ onText, lang, className = '' }: Props) {
  const { supported, listening, error, start, stop } = useSpeechToText(onText, lang)
  if (!supported) return null
  const label = listening ? 'Stop dictation' : 'Dictate with your voice'
  return (
    <span className={`inline-flex flex-col items-start ${className}`}>
      <button type="button" onClick={listening ? stop : start} aria-label={label} title={label} aria-pressed={listening}
        className={`inline-flex items-center justify-center w-9 h-9 rounded-md ring-1 transition-colors ${
          listening ? 'bg-red-500 text-white ring-red-500 animate-pulse' : 'bg-white text-slate-600 ring-slate-300 hover:bg-slate-50'
        }`}>
        <svg viewBox="0 0 24 24" className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
          <rect x="9" y="3" width="6" height="11" rx="3" />
          <path d="M5 11a7 7 0 0 0 14 0M12 18v3" strokeLinecap="round" />
        </svg>
      </button>
      {error && <span role="alert" className="mt-1 text-xs text-red-600">{error}</span>}
    </span>
  )
}

