/** Mesmos limites do backend (`Tag`): conferidos aqui só para dar retorno na hora. */
export const MAX_TAG_NAME_LENGTH = 40
export const MAX_TAGS_PER_ITEM = 10

const VALID_TAG_NAME = /^[\p{L}\p{N}_-]+$/u

/**
 * Mesma normalização do backend, para a tela reconhecer que "#Site Acme" e "site-acme" são a
 * mesma tag antes de enviar: sem "#" no início, minúsculas, espaços viram "-".
 */
export function normalizeTagName(raw: string): string {
  return raw.trim().replace(/^#+\s*/, '').toLowerCase().replace(/\s+/g, '-')
}

/** Mensagem de erro para um nome já normalizado, ou null se ele pode virar tag. */
export function validateTagName(name: string): string | null {
  if (name === '') return 'Informe o nome da tag.'
  if (name.length > MAX_TAG_NAME_LENGTH) return `Use no máximo ${MAX_TAG_NAME_LENGTH} caracteres.`
  if (!VALID_TAG_NAME.test(name)) return 'Use só letras, números, "-" ou "_".'
  return null
}

export function tagLabel(name: string): string {
  return `#${name}`
}
