export interface Theme {
  sidebar: string
  active: string
  hover: string
  accent: string
  page: string
  dense: boolean
}

export const technicianTheme: Theme = {
  sidebar: 'bg-teal-800 text-teal-50',
  active: 'bg-teal-600 text-white',
  hover: 'hover:bg-teal-700',
  accent: 'text-teal-200',
  page: 'bg-slate-50',
  dense: true,
}

export const adminTheme: Theme = {
  sidebar: 'bg-indigo-950 text-indigo-100',
  active: 'bg-indigo-700 text-white',
  hover: 'hover:bg-indigo-900',
  accent: 'text-indigo-300',
  page: 'bg-slate-100',
  dense: false,
}
