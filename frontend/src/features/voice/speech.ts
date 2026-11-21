/**
 * Minimal typings for the Web Speech API. TypeScript's DOM lib doesn't ship them yet,
 * and Chrome/Edge/Safari still expose the constructor under a webkit prefix.
 */
export interface SpeechResult {
  readonly isFinal: boolean
  readonly 0: { readonly transcript: string }
}

export interface SpeechResultEvent extends Event {
  readonly resultIndex: number
  readonly results: { readonly length: number; readonly [index: number]: SpeechResult }
}

export interface SpeechErrorEvent extends Event {
  readonly error: string
}

export interface Recognition extends EventTarget {
  lang: string
  continuous: boolean
  interimResults: boolean
  start(): void
  stop(): void
  abort(): void
  onresult: ((e: SpeechResultEvent) => void) | null
  onerror: ((e: SpeechErrorEvent) => void) | null
  onend: (() => void) | null
}

type RecognitionConstructor = new () => Recognition

export function recognitionConstructor(): RecognitionConstructor | null {
  if (typeof window === 'undefined') return null
  const w = window as unknown as Record<string, RecognitionConstructor | undefined>
  return w.SpeechRecognition ?? w.webkitSpeechRecognition ?? null
}

/** Joins the final transcripts of a result list into one line of text. */
export function finalText(e: SpeechResultEvent): string {
  const parts: string[] = []
  for (let i = e.resultIndex; i < e.results.length; i++) {
    if (e.results[i].isFinal) parts.push(e.results[i][0].transcript.trim())
  }
  return parts.join(' ')
}

export function errorMessage(code: string): string {
  switch (code) {
    case 'not-allowed':
    case 'service-not-allowed':
      return 'Microphone access was blocked. Allow it in the browser to use voice input.'
    case 'no-speech':
      return "Didn't catch anything. Try again a little closer to the mic."
    case 'audio-capture':
      return 'No microphone found.'
    case 'network':
      return 'Voice input needs an internet connection in this browser.'
    default:
      return 'Voice input stopped unexpectedly.'
  }
}

/** Adds a dictated phrase to existing text with a single space between them. */
export const appendSpoken = (current: string, spoken: string) => (current.trim() ? `${current.trimEnd()} ${spoken}` : spoken)
