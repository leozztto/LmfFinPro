import { useQuery } from '@tanstack/react-query'
import { httpClient } from '@/shared/api/httpClient'
import type { Currency } from '@/shared/format/currency'

export interface Conversion {
  from: Currency
  to: Currency
  date: string
  /** Quantas unidades de `to` vale uma de `from`. */
  rate: number
  amount: number
}

/**
 * Conversão pela PTAX do dia, calculada no backend — só para sugerir um valor no formulário (o
 * banco costuma cobrar spread e IOF, então o valor sugerido continua editável). Desligada enquanto
 * faltar algum dado ou as moedas forem iguais.
 */
export function useConversion(from: Currency | undefined, to: Currency | undefined, amount: number, date: string) {
  const enabled = !!from && !!to && from !== to && amount > 0 && /^\d{4}-\d{2}-\d{2}$/.test(date)
  return useQuery({
    queryKey: ['exchange-rates', 'convert', from, to, amount, date],
    queryFn: () =>
      httpClient.get<Conversion>(
        `/exchange-rates/convert?from=${from}&to=${to}&date=${date}&amount=${amount}`,
      ),
    enabled,
    staleTime: 10 * 60 * 1000,
    retry: false,
  })
}
