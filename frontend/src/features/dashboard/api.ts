import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'
import { api, post, type User } from '../../api/client'
import { useLiveEvent } from '../../app/live/LiveContext'

export interface FeedItem {
  type: string
  incidentCode?: string | null
  summary: string
  at: string
}

const FEED_KEY = ['dashboard', 'feed'] as const
const FEED_LIMIT = 50

/** Recent items from the server, kept up to date by 'feed' events on the live stream. */
export function useLiveFeed() {
  const qc = useQueryClient()
  const query = useQuery({ queryKey: FEED_KEY, queryFn: () => api<FeedItem[]>('/admin/feed') })
  const onFeed = useCallback(
    (item: FeedItem) => qc.setQueryData<FeedItem[]>(FEED_KEY, (old = []) => [item, ...old].slice(0, FEED_LIMIT)),
    [qc],
  )
  useLiveEvent('feed', onFeed)
  return query
}

export const useUsers = () => useQuery({ queryKey: ['admin', 'users'], queryFn: () => api<User[]>('/admin/users') })

export const useSendTestNotification = () =>
  useMutation({ mutationFn: (userId: number) => post<void>(`/admin/notifications/test/${userId}`) })
