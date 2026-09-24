import { describe, expect, it } from 'vitest'
import faqContent from '../docs/FAQ.md?raw'
import { normalizeSearchText, parseFaq } from './parseFaq'

describe('parseFaq', () => {
  it('parses the real FAQ.md into every category and question', () => {
    const document = parseFaq(faqContent)

    expect(document.title).toBe('FinPro — Perguntas Frequentes (FAQ)')
    expect(document.subtitle).toBe('Dúvidas comuns de quem usa o FinPro no dia a dia.')
    expect(document.categories).toHaveLength(11)

    const totalItems = document.categories.reduce((sum, category) => sum + category.items.length, 0)
    expect(totalItems).toBe(46)
  })

  it('generates accent-free, hyphenated slugs for categories', () => {
    const document = parseFaq(faqContent)
    const slugs = document.categories.map((category) => category.slug)

    expect(slugs).toContain('conta-e-acesso')
    expect(slugs).toContain('transacoes-receitas-e-despesas')
    expect(new Set(slugs).size).toBe(slugs.length)
  })

  it('keeps markdown formatting inside answers', () => {
    const document = parseFaq(faqContent)
    const category = document.categories.find((c) => c.slug === 'contas-bancarias')

    expect(category?.items[0].answer).toContain('**')
  })

  it('parses a minimal fixture with title, subtitle and one category', () => {
    const fixture = [
      '# Título',
      '___',
      '',
      '*Subtítulo*',
      '___',
      '',
      '## Categoria única',
      '',
      '**Pergunta um?**',
      'Resposta um.',
      '',
      '**Pergunta dois?**',
      'Resposta dois.',
      '___',
      '',
    ].join('\n')

    const document = parseFaq(fixture)

    expect(document.title).toBe('Título')
    expect(document.subtitle).toBe('Subtítulo')
    expect(document.categories).toEqual([
      {
        name: 'Categoria única',
        slug: 'categoria-unica',
        items: [
          { question: 'Pergunta um?', answer: 'Resposta um.' },
          { question: 'Pergunta dois?', answer: 'Resposta dois.' },
        ],
      },
    ])
  })

  it('drops a category left with no valid question/answer pairs', () => {
    const fixture = ['# Título', '___', '', '*Subtítulo*', '___', '', '## Categoria vazia', '___', ''].join('\n')

    expect(parseFaq(fixture).categories).toHaveLength(0)
  })
})

describe('normalizeSearchText', () => {
  it('strips accents and lowercases', () => {
    expect(normalizeSearchText('Crédito e Débito')).toBe('credito e debito')
  })
})
