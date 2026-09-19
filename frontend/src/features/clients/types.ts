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
