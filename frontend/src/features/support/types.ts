export type ServiceState = 'OPERATIONAL' | 'OUTAGE'

/** Só o estado geral: o servidor não detalha componentes. */
export interface PlatformStatus {
  status: ServiceState
  checkedAt: string
}
