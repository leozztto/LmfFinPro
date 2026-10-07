/** Número que cabe numa bolinha pequena: acima de 9 vira "9+". */
export function formatBadgeCount(count: number): string {
  return count > 9 ? '9+' : String(count)
}

/** Texto para leitores de tela: "1 convite pendente", "3 convites pendentes". */
export function pendingInvitesLabel(count: number): string {
  return `${count} ${count === 1 ? 'convite pendente' : 'convites pendentes'}`
}
