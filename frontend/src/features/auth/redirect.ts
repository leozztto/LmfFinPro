/**
 * Caminho interno seguro para voltar depois de entrar (ex.: o convite que a pessoa estava abrindo).
 * Só aceita caminhos do próprio app: nada de "//outro-site" nem URLs completas, para o `state` do
 * roteador não virar um redirecionamento aberto.
 */
export function safeInternalPath(value: unknown): string | null {
  return typeof value === 'string' && /^\/(?![/\\])/.test(value) ? value : null
}
