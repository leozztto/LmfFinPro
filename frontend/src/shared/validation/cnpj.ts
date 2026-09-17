/**
 * Validação de CNPJ (formato + dígitos verificadores), espelhando a mesma regra do backend
 * (com.lmf.finpro.domain.model.CnpjValidator).
 */
const FIRST_WEIGHTS = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]
const SECOND_WEIGHTS = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]

export function isValidCnpj(rawCnpj: string): boolean {
  const cnpj = rawCnpj.replace(/\D/g, '')
  if (cnpj.length !== 14 || hasAllDigitsEqual(cnpj)) {
    return false
  }

  const firstCheckDigit = checkDigit(cnpj.slice(0, 12), FIRST_WEIGHTS)
  const secondCheckDigit = checkDigit(cnpj.slice(0, 12) + firstCheckDigit, SECOND_WEIGHTS)
  return cnpj === cnpj.slice(0, 12) + firstCheckDigit + secondCheckDigit
}

function hasAllDigitsEqual(cnpj: string): boolean {
  return new Set(cnpj.split('')).size === 1
}

function checkDigit(base: string, weights: number[]): number {
  let sum = 0
  for (let i = 0; i < base.length; i++) {
    sum += Number(base[i]) * weights[i]
  }
  const remainder = sum % 11
  return remainder < 2 ? 0 : 11 - remainder
}
