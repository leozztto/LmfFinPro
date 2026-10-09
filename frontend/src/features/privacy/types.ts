export type GroupType = 'PERSONAL' | 'SHARED'

export interface DeletionGroup {
  id: number
  name: string
  type: GroupType
  /** Contas que a pessoa trouxe para o grupo; nos grupos de que ela sai, elas continuam lá. */
  accountsBroughtByYou: number
}

/** O que a exclusão da conta faria, calculado pelo servidor. */
export interface AccountDeletionPreview {
  canDelete: boolean
  /** Motivos que impedem a exclusão agora (ex.: dono de grupo com outros membros). */
  blockers: string[]
  /** Grupos apagados por inteiro: o espaço pessoal e os compartilhados só com a pessoa. */
  deletedGroups: DeletionGroup[]
  /** Grupos compartilhados de que a pessoa sai; os dados continuam com os outros membros. */
  leftGroups: DeletionGroup[]
  attachmentCount: number
}
