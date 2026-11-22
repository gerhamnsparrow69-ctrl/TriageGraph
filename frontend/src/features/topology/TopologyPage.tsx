import { useCallback, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import CytoscapeComponent from 'react-cytoscapejs'
import type cytoscape from 'cytoscape'
import { PageHeader } from '../../app/PageHeader'
import { Card, ErrorNote, Pill, Spinner } from '../triage/ui'
import { catalogApi } from '../catalog/api'
import { topologyApi } from './api'

const stylesheet: cytoscape.StylesheetJsonBlock[] = [
  {
    selector: 'node',
    style: {
      label: 'data(label)',
      color: '#0f172a',
      'font-size': 11,
      'text-wrap': 'wrap',
      'text-max-width': '105px',
      'text-valign': 'center',
      'text-halign': 'center',
      'background-color': '#c7d2fe',
      'border-color': '#6366f1',
      'border-width': 1,
      width: 42,
      height: 42,
    },
  },
  {
    selector: 'node.selected',
    style: {
      'background-color': '#4f46e5',
      'border-color': '#312e81',
      'border-width': 3,
      color: '#ffffff',
    },
  },
  {
    selector: 'edge',
    style: {
      width: 2,
      'line-color': '#94a3b8',
      'target-arrow-color': '#64748b',
      'target-arrow-shape': 'triangle',
      'curve-style': 'bezier',
    },
  },
]

function serviceName(id: number, names: Map<number, string>) {
  return names.get(id) ?? `Service ${id}`
}

function selectedIdOrThrow(id: number | null) {
  if (id === null) {
    throw new Error('Select a service to load its topology details.')
  }
  return id
}

function RelationshipList({
  label,
  serviceIds,
  names,
  onSelect,
}: {
  label: string
  serviceIds: number[]
  names: Map<number, string>
  onSelect: (id: number) => void
}) {
  return (
    <div>
      <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</h3>
      {serviceIds.length === 0 ? (
        <p className="mt-2 text-sm text-slate-500">None</p>
      ) : (
        <ul className="mt-2 flex flex-wrap gap-2">
          {serviceIds.map((id) => (
            <li key={id}>
              <button
                type="button"
                onClick={() => onSelect(id)}
                className="rounded-full bg-slate-100 px-2.5 py-1 text-left text-xs text-slate-700 hover:bg-indigo-100 hover:text-indigo-800"
              >
                {serviceName(id, names)}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default function TopologyPage() {
  const [selectedServiceState, setSelectedServiceState] = useState<number | null>(null)
  const graphQuery = useQuery({
    queryKey: ['topology', 'graph'],
    queryFn: topologyApi.graph,
  })
  const servicesQuery = useQuery({
    queryKey: ['catalog', 'services'],
    queryFn: catalogApi.services,
  })

  const graph = graphQuery.data
  const selectedServiceId = graph?.serviceIds.includes(selectedServiceState ?? -1)
    ? selectedServiceState
    : graph?.serviceIds[0] ?? null
  const serviceNames = useMemo(
    () => new Map((servicesQuery.data ?? []).map((service) => [service.id, service.name])),
    [servicesQuery.data],
  )

  const dependenciesQuery = useQuery({
    queryKey: ['topology', 'dependencies', selectedServiceId],
    queryFn: () => topologyApi.dependencies(selectedIdOrThrow(selectedServiceId)),
    enabled: selectedServiceId !== null,
  })
  const dependentsQuery = useQuery({
    queryKey: ['topology', 'dependents', selectedServiceId],
    queryFn: () => topologyApi.dependents(selectedIdOrThrow(selectedServiceId)),
    enabled: selectedServiceId !== null,
  })
  const blastRadiusQuery = useQuery({
    queryKey: ['topology', 'blast-radius', selectedServiceId],
    queryFn: () => topologyApi.blastRadius(selectedIdOrThrow(selectedServiceId)),
    enabled: selectedServiceId !== null,
  })

  const elements = useMemo<cytoscape.ElementDefinition[]>(() => {
    if (!graph) return []
    const nodes = graph.serviceIds.map((id) => ({
      data: { id: String(id), label: serviceName(id, serviceNames) },
      classes: id === selectedServiceId ? 'selected' : '',
    }))
    const edges = graph.edges.map(({ dependentId, dependencyId }) => ({
      data: {
        id: `${dependentId}-${dependencyId}`,
        source: String(dependentId),
        target: String(dependencyId),
      },
    }))
    return [...nodes, ...edges]
  }, [graph, selectedServiceId, serviceNames])

  const handleGraphReady = useCallback((cy: cytoscape.Core) => {
    cy.on('tap', 'node', (event) => {
      const id = Number(event.target.id())
      if (Number.isSafeInteger(id)) setSelectedServiceState(id)
    })
  }, [])

  return (
    <>
      <PageHeader
        title="Service dependency graph"
        subtitle="Select a service node to inspect its dependencies, dependents, and blast radius."
      />

      <div className="space-y-5">
        <Card
          title="Dependency graph"
          subtitle={graph ? `${graph.serviceIds.length} services · ${graph.edgeCount} dependencies · version ${graph.version}` : undefined}
        >
          {graphQuery.isPending && <Spinner label="Loading dependency graph…" />}
          {graphQuery.isError && <ErrorNote error={graphQuery.error} />}
          {servicesQuery.isPending && !servicesQuery.data && (
            <Spinner label="Loading service names…" />
          )}
          {servicesQuery.isError && <ErrorNote error={servicesQuery.error} />}
          {graph && graph.serviceIds.length === 0 && (
            <p className="py-6 text-center text-sm text-slate-500">No services are in the dependency graph.</p>
          )}
          {graph && graph.serviceIds.length > 0 && (
            <>
              <div className="overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
                <CytoscapeComponent
                  elements={elements}
                  stylesheet={stylesheet}
                  layout={{ name: 'cose', animate: false, padding: 32 }}
                  cy={handleGraphReady}
                  style={{ width: '100%', height: 'clamp(24rem, 58vh, 42rem)' }}
                  minZoom={0.3}
                  maxZoom={2}
                  wheelSensitivity={0.2}
                />
              </div>
              <p className="mt-3 text-xs text-slate-500">
                Arrows point from a service to a service it depends on. Drag to arrange; scroll or pinch to zoom.
              </p>
            </>
          )}
        </Card>

        {selectedServiceId !== null && (
          <Card
            title={serviceName(selectedServiceId, serviceNames)}
            subtitle={`Service ID ${selectedServiceId}`}
          >
            {dependenciesQuery.isPending || dependentsQuery.isPending || blastRadiusQuery.isPending ? (
              <Spinner label="Loading service impact details…" />
            ) : (
              <div className="space-y-5">
                {dependenciesQuery.isError && <ErrorNote error={dependenciesQuery.error} />}
                {dependentsQuery.isError && <ErrorNote error={dependentsQuery.error} />}
                {blastRadiusQuery.isError && <ErrorNote error={blastRadiusQuery.error} />}

                {blastRadiusQuery.data && (
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-slate-600">Blast radius</span>
                    <Pill tone={blastRadiusQuery.data.dependentCount > 0 ? 'amber' : 'green'}>
                      {blastRadiusQuery.data.dependentCount} dependent services
                    </Pill>
                  </div>
                )}

                <div className="grid gap-5 md:grid-cols-2">
                  {dependenciesQuery.data && (
                    <div className="space-y-4">
                      <h2 className="font-semibold text-slate-900">Dependencies</h2>
                      <RelationshipList
                        label="Direct"
                        serviceIds={dependenciesQuery.data.directServiceIds}
                        names={serviceNames}
                        onSelect={setSelectedServiceState}
                      />
                      <RelationshipList
                        label="Transitive"
                        serviceIds={dependenciesQuery.data.transitiveServiceIds}
                        names={serviceNames}
                        onSelect={setSelectedServiceState}
                      />
                    </div>
                  )}
                  {dependentsQuery.data && (
                    <div className="space-y-4">
                      <h2 className="font-semibold text-slate-900">Dependents</h2>
                      <RelationshipList
                        label="Direct"
                        serviceIds={dependentsQuery.data.directServiceIds}
                        names={serviceNames}
                        onSelect={setSelectedServiceState}
                      />
                      <RelationshipList
                        label="Transitive"
                        serviceIds={dependentsQuery.data.transitiveServiceIds}
                        names={serviceNames}
                        onSelect={setSelectedServiceState}
                      />
                    </div>
                  )}
                </div>
              </div>
            )}
          </Card>
        )}
      </div>
    </>
  )
}
