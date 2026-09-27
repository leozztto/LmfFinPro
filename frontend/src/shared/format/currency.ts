/** Moedas aceitas nas contas e nas transações; BRL é a moeda de consolidação. */
export type Currency = 'BRL' | 'USD' | 'EUR'

export const CURRENCIES: Currency[] = ['BRL', 'USD', 'EUR']

export const CURRENCY_LABELS: Record<Currency, string> = {
  BRL: 'Real (R$)',
  USD: 'Dólar (US$)',
  EUR: 'Euro (€)',
}

const formatters = new Map<Currency, Intl.NumberFormat>()

function formatterFor(currency: Currency): Intl.NumberFormat {
  let formatter = formatters.get(currency)
  if (!formatter) {
    formatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency })
    formatters.set(currency, formatter)
  }
  return formatter
}

export function formatCurrency(value: number, currency: Currency = 'BRL'): string {
  return formatterFor(currency).format(value)
}
