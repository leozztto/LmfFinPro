/**
 * Regra de exibição da autoria: numa conta compartilhada só quem criou um lançamento ou uma
 * transferência pode excluí-lo. É só para a tela desabilitar o botão e explicar o motivo — quem
 * de fato impede a exclusão é o servidor (resposta 403). Sem autor conhecido (criado pelo sistema,
 * como uma recorrência), qualquer membro do grupo pode excluir.
 */
export function canDeleteRecord(createdByUserId: number | null | undefined, currentUserId: number | undefined): boolean {
  return createdByUserId == null || createdByUserId === currentUserId
}

/** O que mostrar no botão de excluir quando ele está bloqueado; null quando pode excluir. */
export function deleteBlockedReason(
  createdByUserId: number | null | undefined,
  createdByName: string | null | undefined,
  currentUserId: number | undefined,
  noun: 'lançamento' | 'transferência',
): string | null {
  if (canDeleteRecord(createdByUserId, currentUserId)) return null
  const who = createdByName ? `${createdByName}, que criou` : 'a pessoa que criou'
  return `Só ${who} ${noun === 'lançamento' ? 'este lançamento' : 'esta transferência'}, pode excluí-${noun === 'lançamento' ? 'lo' : 'la'}.`
}
