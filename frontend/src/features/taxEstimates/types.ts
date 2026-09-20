export type TaxRegime = 'AUTONOMO' | 'MEI' | 'SIMPLES_NACIONAL' | 'LUCRO_PRESUMIDO' | 'OUTRO'

export interface TaxEstimate {
  id: number
  referenceMonth: string
  regime: TaxRegime
  grossRevenue: number
  appliedRate: number
  estimatedValue: number
}

export interface TaxEstimateInput {
  referenceMonth: string
  regime: TaxRegime
  grossRevenue: number
  appliedRate: number
}

export const TAX_REGIME_LABELS: Record<TaxRegime, string> = {
  AUTONOMO: 'Autônomo / pessoa física',
  MEI: 'MEI',
  SIMPLES_NACIONAL: 'Simples Nacional',
  LUCRO_PRESUMIDO: 'Lucro presumido',
  OUTRO: 'Outro',
}
