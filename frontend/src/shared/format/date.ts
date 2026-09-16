/**
 * Formata uma data "somente data" (ISO "YYYY-MM-DD", sem horário) como DD/MM/YYYY.
 * Evita usar `new Date(isoDate)`, que interpreta a string como UTC e desloca o dia
 * exibido para trás em fusos horários negativos (ex: América/São_Paulo).
 */
export function formatDateOnlyBr(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

/** "YYYY-MM" do mês atual, calculado a partir da data local (não UTC) do dispositivo. */
export function getCurrentYearMonth(): string {
  return getCurrentIsoDate().slice(0, 7)
}

/** "YYYY-MM-DD" de hoje, calculado a partir da data local (não UTC) do dispositivo. */
export function getCurrentIsoDate(): string {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}
