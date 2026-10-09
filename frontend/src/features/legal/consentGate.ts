/**
 * Telas que continuam abertas enquanto o aceite dos documentos está pendente: sem aceitar, a pessoa
 * ainda pode baixar os dados e excluir a conta (LGPD, art. 18).
 */
const EXEMPT_PATH_PREFIXES = ['/configuracoes/privacidade']

export function isConsentScreenExempt(pathname: string): boolean {
  return EXEMPT_PATH_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`))
}
