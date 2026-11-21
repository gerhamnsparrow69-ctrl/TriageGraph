import { useQueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'
import { useLiveEvent } from '../../app/live/LiveContext'
import { useToast } from '../../app/ui/Toasts'
import type { NotificationItem } from './api'

/** Refreshes the bell and shows a toast whenever a notification arrives on the live stream. */
export function useNotificationStream() {
  const qc = useQueryClient()
  const toast = useToast()
  const onNotification = useCallback(
    (n: NotificationItem) => {
      qc.invalidateQueries({ queryKey: ['notifications'] })
      toast(n.subject, n.priority === 'P1' ? 'error' : 'info')
    },
    [qc, toast],
  )
  useLiveEvent('notification', onNotification)
}
