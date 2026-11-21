/** Formatting helpers shared by Member 2's pages. */

export const percent = (v: number) => `${Math.round(v * 100)}%`

export function minutesAgo(m: number): string {
  if (m < 1) return 'just now'
  if (m < 60) return `${m} min ago`
  const h = Math.round(m / 60)
  if (h < 48) return `${h} h ago`
  return `${Math.round(h / 24)} days ago`
}

export function sourceLabel(source: string): string {
  switch (source) {
    case 'LLM':
      return 'Gemini'
    case 'KNN':
      return 'Similar incidents'
    case 'KEYWORD':
      return 'Keyword rules'
    case 'USER':
      return 'You'
    case 'FALLBACK':
      return 'Default'
    default:
      return source
  }
}

export function title(code: string): string {
  return code.charAt(0) + code.slice(1).toLowerCase().replace(/_/g, ' ')
}
