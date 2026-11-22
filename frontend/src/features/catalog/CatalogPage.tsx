import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '../../app/PageHeader'
import { Card, ErrorNote, Pill, Spinner } from '../triage/ui'
import { catalogApi, type Criticality, type ServiceRef } from './api'

const criticalityTone: Record<Criticality, 'slate' | 'blue' | 'amber' | 'red'> = {
  LOW: 'slate',
  MEDIUM: 'blue',
  HIGH: 'amber',
  MISSION_CRITICAL: 'red',
}

function humanise(value: string) {
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}

function ServiceDetails({ service }: { service: ServiceRef }) {
  return (
    <dl className="grid gap-3 text-sm sm:grid-cols-2">
      <div>
        <dt className="text-xs uppercase tracking-wide text-slate-500">Service ID</dt>
        <dd className="mt-1 font-mono text-slate-800">{service.id}</dd>
      </div>
      <div>
        <dt className="text-xs uppercase tracking-wide text-slate-500">Code</dt>
        <dd className="mt-1 font-mono text-slate-800">{service.code}</dd>
      </div>
      <div>
        <dt className="text-xs uppercase tracking-wide text-slate-500">Name</dt>
        <dd className="mt-1 font-medium text-slate-900">{service.name}</dd>
      </div>
      <div>
        <dt className="text-xs uppercase tracking-wide text-slate-500">Criticality</dt>
        <dd className="mt-1">
          <Pill tone={criticalityTone[service.criticality]}>{humanise(service.criticality)}</Pill>
        </dd>
      </div>
    </dl>
  )
}

export default function CatalogPage() {
  const [selectedServiceId, setSelectedServiceId] = useState<number | null>(null)
  const servicesQuery = useQuery({
    queryKey: ['catalog', 'services'],
    queryFn: catalogApi.services,
  })
  const categoriesQuery = useQuery({
    queryKey: ['catalog', 'categories'],
    queryFn: catalogApi.categories,
  })
  const serviceDetailsQuery = useQuery({
    queryKey: ['catalog', 'service', selectedServiceId],
    queryFn: () => {
      if (selectedServiceId === null) {
        throw new Error('Select a service to load its details.')
      }
      return catalogApi.service(selectedServiceId)
    },
    enabled: selectedServiceId !== null,
  })

  return (
    <>
      <PageHeader
        title="Service catalogue"
        subtitle="Browse available services, their criticality, and active support categories."
      />

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.3fr)_minmax(18rem,0.7fr)]">
        <div className="space-y-5">
          <Card
            title="Services"
            subtitle={servicesQuery.data ? `${servicesQuery.data.length} services` : undefined}
          >
            <p className="mb-4 text-xs text-slate-500">
              Categories are listed separately; the catalogue API does not associate categories with services.
            </p>
            {servicesQuery.isPending && <Spinner label="Loading services…" />}
            {servicesQuery.isError && <ErrorNote error={servicesQuery.error} />}
            {servicesQuery.data?.length === 0 && (
              <p className="py-4 text-sm text-slate-500">No services are available.</p>
            )}
            {servicesQuery.data && servicesQuery.data.length > 0 && (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-3 py-2.5 font-medium">Service</th>
                      <th className="px-3 py-2.5 font-medium">Code</th>
                      <th className="px-3 py-2.5 font-medium">Criticality</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {servicesQuery.data.map((service) => {
                      const selected = service.id === selectedServiceId
                      return (
                        <tr key={service.id} className={selected ? 'bg-indigo-50' : 'hover:bg-slate-50'}>
                          <td className="px-3 py-2.5">
                            <button
                              type="button"
                              aria-pressed={selected}
                              onClick={() => setSelectedServiceId(service.id)}
                              className="text-left font-medium text-indigo-700 hover:underline"
                            >
                              {service.name}
                            </button>
                          </td>
                          <td className="px-3 py-2.5 font-mono text-xs text-slate-600">{service.code}</td>
                          <td className="px-3 py-2.5">
                            <Pill tone={criticalityTone[service.criticality]}>
                              {humanise(service.criticality)}
                            </Pill>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card
            title="Categories"
            subtitle={categoriesQuery.data ? `${categoriesQuery.data.length} active categories` : undefined}
          >
            {categoriesQuery.isPending && <Spinner label="Loading categories…" />}
            {categoriesQuery.isError && <ErrorNote error={categoriesQuery.error} />}
            {categoriesQuery.data?.length === 0 && (
              <p className="py-4 text-sm text-slate-500">No active categories are available.</p>
            )}
            {categoriesQuery.data && categoriesQuery.data.length > 0 && (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-3 py-2.5 font-medium">Category</th>
                      <th className="px-3 py-2.5 font-medium">Code</th>
                      <th className="px-3 py-2.5 font-medium">Default skill</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {categoriesQuery.data.map((category) => (
                      <tr key={category.code}>
                        <td className="px-3 py-2.5 font-medium text-slate-900">{category.name}</td>
                        <td className="px-3 py-2.5 font-mono text-xs text-slate-600">{category.code}</td>
                        <td className="px-3 py-2.5 text-slate-600">{category.defaultSkill ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </div>

        <Card
          title="Service details"
          subtitle="Select a service to view its catalogue record."
          actions={selectedServiceId !== null && (
            <button
              type="button"
              onClick={() => setSelectedServiceId(null)}
              className="text-sm text-slate-600 hover:text-slate-900 hover:underline"
            >
              Clear
            </button>
          )}
        >
          {selectedServiceId === null && (
            <p className="py-4 text-sm text-slate-500">No service selected.</p>
          )}
          {selectedServiceId !== null && serviceDetailsQuery.isPending && (
            <Spinner label="Loading service details…" />
          )}
          {selectedServiceId !== null && serviceDetailsQuery.isError && (
            <ErrorNote error={serviceDetailsQuery.error} />
          )}
          {serviceDetailsQuery.data && <ServiceDetails service={serviceDetailsQuery.data} />}
        </Card>
      </div>
    </>
  )
}
