export interface AuthSession {
  token: string
  userId: number
  name: string
  email: string
}

export type DocumentType = 'CPF' | 'CNPJ'

export interface AddressPayload {
  zipCode: string
  street: string
  number: string
  complement?: string
  neighborhood: string
  city: string
  state: string
}

export interface RegisterPayload {
  name: string
  email: string
  password: string
  documentType: DocumentType
  documentNumber: string
  phone?: string
  taxRegime?: string
  address: AddressPayload
}

export interface LoginPayload {
  email: string
  password: string
}
