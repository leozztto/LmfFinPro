/**
 * Paleta categórica fixa do design system (ordem estável — a cor segue a entidade, nunca a posição
 * no ranking). Validada com o validador da skill de dataviz nas superfícies dos cards (#fafafa no
 * claro, #27272a no escuro): os 5 primeiros passam em faixa de luminosidade, croma, separação para
 * daltonismo e visão normal; no claro, o contraste de verde/amarelo/rosa fica abaixo de 3:1 contra
 * o fundo, por isso os gráficos que usam essa paleta sempre têm legenda e uma tabela com os mesmos
 * dados.
 */
export const CATEGORICAL_PALETTE_LIGHT = [
  '#2a78d6',
  '#eb6834',
  '#1baf7a',
  '#eda100',
  '#e87ba4',
  '#008300',
  '#4a3aa7',
  '#e34948',
]

export const CATEGORICAL_PALETTE_DARK = [
  '#3987e5',
  '#d95926',
  '#199e70',
  '#c98500',
  '#d55181',
  '#008300',
  '#9085e9',
  '#e66767',
]

/** Cinza neutro para "Outros" / "Sem cliente" — nunca uma cor categórica. */
export const NEUTRAL_SERIES_COLOR = '#94a3b8'
