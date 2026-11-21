import { useState } from 'react'
import { useToast } from '../../app/ui/Toasts'
import { useSendTestNotification, useUsers } from './api'

/** Lets an admin check that in-app, email and push all reach a user (TC20 demo). */
export function TestNotificationCard() {
  const { data: users = [] } = useUsers()
  const send = useSendTestNotification()
  const toast = useToast()
  const [userId, setUserId] = useState('')

  return (
    <section className="rounded-xl bg-white ring-1 ring-slate-200 p-4">
      <h2 className="font-semibold text-slate-900">Test notifications</h2>
      <p className="text-sm text-slate-500 mt-1">Sends a test on every channel and records it in the delivery log.</p>
      <form
        className="mt-3 flex flex-col sm:flex-row gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          if (!userId) return
          send.mutate(Number(userId), {
            onSuccess: () => toast('Test notification sent', 'success'),
            onError: (err) => toast(err.message, 'error'),
          })
        }}
      >
        <label className="sr-only" htmlFor="test-user">User</label>
        <select id="test-user" value={userId} onChange={(e) => setUserId(e.target.value)}
          className="flex-1 min-w-0 rounded-md border border-slate-300 px-3 py-2 text-sm bg-white">
          <option value="">Choose a user…</option>
          {users.filter((u) => u.active).map((u) => (
            <option key={u.id} value={u.id}>{u.name} ({u.role?.toLowerCase().replace('_', ' ')})</option>
          ))}
        </select>
        <button type="submit" disabled={!userId || send.isPending}
          className="rounded-md bg-indigo-600 text-white px-4 py-2 text-sm font-medium hover:bg-indigo-700 disabled:opacity-50">
          {send.isPending ? 'Sending…' : 'Send test'}
        </button>
      </form>
    </section>
  )
}
