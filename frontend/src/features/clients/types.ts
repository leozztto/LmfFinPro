import type { DocumentType } from '@/shared/auth/types'

export type ClientWorkType = 'PJ' | 'AUTONOMO'

export interface Client {
  id: number
  name: string
  email: string | null
  phone: string | null
  documentType: DocumentType | null
  documentNumber: string | null
  workType: ClientWorkType | null
  notes: string | null
  color: string | null
  active: boolean
}

export interface ClientInput {
  name: string
  email?: string
  phone?: string
  documentType?: DocumentType
  documentNumber?: string
  workType?: ClientWorkType
  notes?: string
  color?: string
  active: boolean
}

export const CLIENT_WORK_TYPE_LABELS: Record<ClientWorkType, string> = {
  PJ: 'PJ',
  AUTONOMO: 'Autônomo/Freelancer',
}

export type ConcentrationRisk = 'NONE' | 'LOW' | 'MODERATE' | 'HIGH'

export interface ClientMonthValues {
  month: string
  income: number
  expense: number
}

/** Um cliente no ranking do período; os valores vêm prontos do backend. */
export interface ClientRankingRow {
  clientId: number
  name: string
  color: string | null
  income: number
  expense: number
  net: number
  incomeCount: number
  averageTicket: number
  /** Fração (0 a 1) da receita total do período. */
  share: number
  activeMonths: number
  lastIncomeDate: string | null
  monthly: ClientMonthValues[]
}

export interface ClientAnalytics {
  months: string[]
  totalIncome: number
  /** Receitas sem cliente: entram no total (e na concentração), mas não no ranking. */
  unassignedIncome: number
  activeClients: number
  averageTicket: number
  topClientShare: number
  topThreeShare: number
  risk: ConcentrationRisk
  ranking: ClientRankingRow[]
}
