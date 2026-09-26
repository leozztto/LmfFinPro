import type { AddressPayload, DocumentType } from '@/shared/auth/types'

export interface Profile {
  id: number
  name: string
  email: string
  documentType: DocumentType
  documentNumber: string
  phone: string | null
  taxRegime: string
  address: AddressPayload | null
  createdAt: string
}

export interface UpdateProfileInput {
  name: string
  email: string
  documentType: DocumentType
  documentNumber: string
  phone?: string
  taxRegime: string
  address: AddressPayload
  /** Obrigatória só quando o e-mail muda. */
  currentPassword?: string
}

export interface ChangePasswordInput {
  currentPassword: string
  newPassword: string
}

export interface NotificationPreferences {
  billsEnabled: boolean
  /** Antecedência, em dias, do aviso de contas a vencer e do DAS (0 = só no dia). */
  billDaysBefore: number
  budgetsEnabled: boolean
  dasEnabled: boolean
}

export interface ChangePasswordResponse {
  token: string
  userId: number
  name: string
  email: string
}
