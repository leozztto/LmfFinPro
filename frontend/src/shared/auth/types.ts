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
  /** Token de um convite para grupo (link do e-mail): ao criar a conta a pessoa já entra no grupo. */
  inviteToken?: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface CepAddress {
  zipCode: string
  street: string
  complement?: string
  neighborhood: string
  city: string
  state: string
}
