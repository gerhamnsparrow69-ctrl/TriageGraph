import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, post, put, type Role, type User } from '../../api/client'

export const userKeys = {
  all: ['admin', 'users'] as const,
  technicians: ['admin', 'technicians'] as const,
}

export interface NewUserInput {
  role: Role
  name: string
  email: string
  password: string
  department?: string
  skills?: string[]
  experienceYears: number
  maxLoad: number
}

export interface ProfileInput {
  name: string
  department?: string
  pushTopic?: string
}

export interface TechnicianInput {
  skills: string[]
  experienceYears: number
  maxLoad: number
  available: boolean
  teamLeadId?: number | null
}

export const useUsers = () => useQuery({ queryKey: userKeys.all, queryFn: () => api<User[]>('/admin/users') })

export const useTechnicians = () =>
  useQuery({ queryKey: userKeys.technicians, queryFn: () => api<User[]>('/admin/technicians') })

/** Any change to a person can show up on both pages, so refresh both lists. */
function useRefresh() {
  const qc = useQueryClient()
  return () => qc.invalidateQueries({ queryKey: ['admin'] })
}

export function useCreateUser() {
  const refresh = useRefresh()
  return useMutation({ mutationFn: (u: NewUserInput) => post<User>('/admin/users', u), onSuccess: refresh })
}

export function useUpdateProfile() {
  const refresh = useRefresh()
  return useMutation({
    mutationFn: ({ id, ...body }: ProfileInput & { id: number }) => put<User>(`/admin/users/${id}`, body),
    onSuccess: refresh,
  })
}

export function useSetActive() {
  const refresh = useRefresh()
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      post<User>(`/admin/users/${id}/${active ? 'activate' : 'deactivate'}`),
    onSuccess: refresh,
  })
}

export function useUpdateTechnician() {
  const refresh = useRefresh()
  return useMutation({
    mutationFn: ({ id, ...body }: TechnicianInput & { id: number }) => put<User>(`/admin/technicians/${id}`, body),
    onSuccess: refresh,
  })
}
