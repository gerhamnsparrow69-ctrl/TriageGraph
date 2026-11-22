import { api } from '../../api/client'

export interface DependencyEdge {
  dependentId: number
  dependencyId: number
}

export interface GraphResponse {
  serviceIds: number[]
  edges: DependencyEdge[]
  edgeCount: number
  version: number
}

export interface RelationshipsResponse {
  serviceId: number
  directServiceIds: number[]
  transitiveServiceIds: number[]
}

export interface BlastRadiusResponse {
  serviceId: number
  dependentCount: number
}

export const topologyApi = {
  graph: () => api<GraphResponse>('/topology'),
  dependencies: (serviceId: number) =>
    api<RelationshipsResponse>(`/topology/services/${serviceId}/dependencies`),
  dependents: (serviceId: number) =>
    api<RelationshipsResponse>(`/topology/services/${serviceId}/dependents`),
  blastRadius: (serviceId: number) =>
    api<BlastRadiusResponse>(`/topology/services/${serviceId}/blast-radius`),
}
