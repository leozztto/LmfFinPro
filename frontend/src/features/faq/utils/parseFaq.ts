import type { FaqCategory, FaqDocument, FaqItem } from '../types'

const BLOCK_DELIMITER = /\r?\n___\r?\n/
const PARAGRAPH_DELIMITER = /\r?\n\s*\r?\n/
const QUESTION_PATTERN = /^\*\*(.+)\*\*$/s

/** Remove acentos e normaliza caixa, para permitir busca insensível a acentuação. */
export function normalizeSearchText(text: string): string {
  return text
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
}

function slugify(name: string): string {
  return normalizeSearchText(name)
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

function splitParagraphs(block: string): string[] {
  return block
    .split(PARAGRAPH_DELIMITER)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean)
}

function parseItem(paragraph: string): FaqItem | null {
  const newlineIndex = paragraph.indexOf('\n')
  if (newlineIndex === -1) return null

  const questionLine = paragraph.slice(0, newlineIndex).trim()
  const answer = paragraph.slice(newlineIndex + 1).trim()
  const questionMatch = QUESTION_PATTERN.exec(questionLine)
  if (!questionMatch || !answer) return null

  return { question: questionMatch[1].trim(), answer }
}

function parseCategory(block: string): FaqCategory | null {
  const paragraphs = splitParagraphs(block)
  const [heading, ...rest] = paragraphs
  if (!heading) return null

  const name = heading.replace(/^##\s*/, '').trim()
  const slug = slugify(name)

  const items: FaqItem[] = []
  for (const paragraph of rest) {
    const item = parseItem(paragraph)
    if (!item) {
      console.warn(`[FAQ] Parágrafo inesperado (esperava "**pergunta**\\nresposta") na categoria "${name}": "${paragraph}"`)
      continue
    }
    items.push(item)
  }

  return { name, slug, items }
}

export function parseFaq(markdown: string): FaqDocument {
  const blocks = markdown
    .split(BLOCK_DELIMITER)
    .map((block) => block.trim())
    .filter(Boolean)

  const [titleBlock, subtitleBlock, ...categoryBlocks] = blocks

  const title = (titleBlock ?? '').replace(/^#\s*/, '').trim()
  const subtitle = (subtitleBlock ?? '').replace(/^\*|\*$/g, '').trim()
  const categories = categoryBlocks
    .map(parseCategory)
    .filter((category): category is FaqCategory => category != null && category.items.length > 0)

  return { title, subtitle, categories }
}
