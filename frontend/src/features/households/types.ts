/** PERSONAL: o espaço só da pessoa (criado no cadastro). SHARED: grupo de casal/família. */
export type HouseholdType = 'PERSONAL' | 'SHARED'

/** Papel de quem consulta naquele grupo: o dono gerencia os membros; ambos veem e editam os dados. */
export type HouseholdRole = 'OWNER' | 'MEMBER'

export interface Household {
  id: number
  name: string
  type: HouseholdType
  role: HouseholdRole
}

export interface HouseholdMember {
  userId: number
  name: string
  email: string
  role: HouseholdRole
}

export interface HouseholdInvite {
  id: number
  email: string
  expiresAt: string
  createdAt: string
}

/** O que passou do espaço pessoal para o grupo. */
export interface AccountSharingResult {
  accounts: number
  transactions: number
}

/** Convite pendente endereçado ao e-mail da conta, para aceitar ou recusar sem precisar do link. */
export interface ReceivedInvite {
  id: number
  householdId: number
  householdName: string
  inviterName: string
  expiresAt: string
}

export const HOUSEHOLD_ROLE_LABELS: Record<HouseholdRole, string> = {
  OWNER: 'Dono',
  MEMBER: 'Membro',
}
