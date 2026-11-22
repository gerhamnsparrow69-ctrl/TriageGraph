import { api } from '../../api/client'

export type Priority = 'P1' | 'P2' | 'P3' | 'P4'

export interface SlaTargets {
  response: string
  resolution: string
  warningPercent: number
  policyName: string
}

export const slaApi = {
  targets: (serviceId: number, priority: Priority) => {
    const params = new URLSearchParams({
      serviceId: String(serviceId),
      priority,
    })
    return api<SlaTargets>(`/sla/targets?${params.toString()}`)
  },
}
