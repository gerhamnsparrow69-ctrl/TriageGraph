import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '../../app/PageHeader'
import { catalogApi } from '../catalog/api'
import { Card, ErrorNote, Pill, Spinner } from '../triage/ui'
import { slaApi, type Priority } from './api'

const priorities: Priority[] = ['P1', 'P2', 'P3', 'P4']

function formatDuration(value: string) {
  const match = /^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?$/.exec(value)
  if (!match) return value

  const [, hours, minutes, seconds] = match
  const parts: string[] = []
  if (hours) parts.push(`${hours} ${Number(hours) === 1 ? 'hour' : 'hours'}`)
  if (minutes) parts.push(`${minutes} ${Number(minutes) === 1 ? 'minute' : 'minutes'}`)
  if (seconds) parts.push(`${seconds} ${Number(seconds) === 1 ? 'second' : 'seconds'}`)
  return parts.length > 0 ? parts.join(' ') : '0 seconds'
}

export default function SlaPoliciesPage() {
  const [serviceId, setServiceId] = useState('')
  const [priority, setPriority] = useState<Priority>('P1')

  const servicesQuery = useQuery({
    queryKey: ['catalog', 'services'],
    queryFn: catalogApi.services,
  })
  const targetsQuery = useQuery({
    queryKey: ['sla', 'targets', serviceId, priority],
    queryFn: () => slaApi.targets(Number(serviceId), priority),
    enabled: serviceId !== '',
  })
  const selectedService = servicesQuery.data?.find((service) => String(service.id) === serviceId)

  return (
    <>
      <PageHeader
        title="SLA policies"
        subtitle="Review response and resolution targets by service and incident priority."
      />

      <div className="space-y-5">
        <Card title="Select policy scope">
          {servicesQuery.isPending && <Spinner label="Loading services…" />}
          {servicesQuery.isError && <ErrorNote error={servicesQuery.error} />}
          {servicesQuery.data?.length === 0 && (
            <p className="text-sm text-slate-500">No services are available to configure SLA targets.</p>
          )}
          {servicesQuery.data && servicesQuery.data.length > 0 && (
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block text-sm font-medium text-slate-700">
                Service
                <select
                  value={serviceId}
                  onChange={(event) => setServiceId(event.target.value)}
                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-normal text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  <option value="">Choose a service</option>
                  {servicesQuery.data.map((service) => (
                    <option key={service.id} value={service.id}>
                      {service.name}
                    </option>
                  ))}
                </select>
              </label>

              <label className="block text-sm font-medium text-slate-700">
                Priority
                <select
                  value={priority}
                  onChange={(event) => setPriority(event.target.value as Priority)}
                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-normal text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  {priorities.map((value) => (
                    <option key={value} value={value}>{value}</option>
                  ))}
                </select>
              </label>
            </div>
          )}
        </Card>

        <Card
          title="SLA targets"
          subtitle={selectedService ? `${selectedService.name} · ${priority}` : 'Choose a service to view its targets.'}
        >
          {!serviceId && (
            <p className="py-4 text-sm text-slate-500">Select a service to load its SLA policy.</p>
          )}
          {serviceId && targetsQuery.isPending && <Spinner label="Loading SLA targets…" />}
          {serviceId && targetsQuery.isError && <ErrorNote error={targetsQuery.error} />}
          {targetsQuery.data && (
            <div className="space-y-5">
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Response target</p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">
                    {formatDuration(targetsQuery.data.response)}
                  </p>
                </div>
                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Resolution target</p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">
                    {formatDuration(targetsQuery.data.resolution)}
                  </p>
                </div>
                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Warning threshold</p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">
                    {targetsQuery.data.warningPercent}%
                  </p>
                </div>
                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Policy</p>
                  <p className="mt-2">
                    <Pill tone="blue">{targetsQuery.data.policyName}</Pill>
                  </p>
                </div>
              </div>
            </div>
          )}
        </Card>
      </div>
    </>
  )
}
