/**
 * Validação de CPF (formato + dígitos verificadores), espelhando a mesma regra do backend
 * (com.lmf.finpro.domain.model.CpfValidator) para dar feedback imediato no formulário —
 * o backend sempre revalida antes de persistir.
 */
export function isValidCpf(rawCpf: string): boolean {
  const cpf = rawCpf.replace(/\D/g, '')
  if (cpf.length !== 11 || hasAllDigitsEqual(cpf)) {
    return false
  }

  const firstCheckDigit = checkDigit(cpf.slice(0, 9), 10)
  const secondCheckDigit = checkDigit(cpf.slice(0, 9) + firstCheckDigit, 11)
  return cpf === cpf.slice(0, 9) + firstCheckDigit + secondCheckDigit
}

function hasAllDigitsEqual(cpf: string): boolean {
  return new Set(cpf.split('')).size === 1
}

function checkDigit(base: string, startingWeight: number): number {
  let weight = startingWeight
  let sum = 0
  for (const char of base) {
    sum += Number(char) * weight--
  }
  const remainder = sum % 11
  return remainder < 2 ? 0 : 11 - remainder
}
