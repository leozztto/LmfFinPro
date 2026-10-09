import { describe, expect, it } from 'vitest'
import { insightMessage } from './insights'

const normalize = (text: string) => text.replace(/\s/g, ' ')

describe('insightMessage', () => {
  it('descreve a assinatura esquecida', () => {
    const text = normalize(
      insightMessage({ type: 'SUBSCRIPTION', subject: 'Netflix', amount: 55.9, reference: null, count: 5 }),
    )
    expect(text).toContain('Netflix: cobrado há 5 meses seguidos')
    expect(text).toContain('R$ 55,90/mês')
  })

  it('compara a despesa com a média da categoria', () => {
    const text = normalize(
      insightMessage({ type: 'UNUSUAL_EXPENSE', subject: 'Churrasco (Lazer)', amount: 480, reference: 100, count: 0 }),
    )
    expect(text).toContain('R$ 480,00')
    expect(text).toContain('média de R$ 100,00')
  })

  it('soma os recebimentos atrasados do cliente', () => {
    const text = normalize(
      insightMessage({ type: 'LATE_CLIENT', subject: 'Acme', amount: 1500, reference: null, count: 2 }),
    )
    expect(text).toContain('Acme atrasou 2 recebimentos')
    expect(text).toContain('R$ 1.500,00')
  })
})
