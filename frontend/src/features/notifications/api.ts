import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, post } from '../../api/client'

export interface NotificationItem {
  id: number
  subject: string
  body: string
  priority?: 'P1' | 'P2' | 'P3' | 'P4' | null
  eventType: string
  incidentId?: number | null
  createdAt: string
  read: boolean
}

export const keys = {
  list: ['notifications', 'list'] as const,
  unread: ['notifications', 'unread'] as const,
}

export const useNotifications = () =>
  useQuery({ queryKey: keys.list, queryFn: () => api<NotificationItem[]>('/notifications/me') })

export const useUnreadCount = () =>
  useQuery({
    queryKey: keys.unread,
    queryFn: () => api<{ unread: number }>('/notifications/me/unread-count'),
    select: (d) => d.unread,
  })

function useInvalidate() {
  const qc = useQueryClient()
  return () => qc.invalidateQueries({ queryKey: ['notifications'] })
}

export function useMarkRead() {
  const invalidate = useInvalidate()
  return useMutation({ mutationFn: (id: number) => post(`/notifications/${id}/read`), onSuccess: invalidate })
}

export function useMarkAllRead() {
  const invalidate = useInvalidate()
  return useMutation({ mutationFn: () => post('/notifications/me/read-all'), onSuccess: invalidate })
}
