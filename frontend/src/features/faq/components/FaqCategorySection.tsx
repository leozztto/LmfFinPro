import ReactMarkdown from 'react-markdown'
import { AccordionItem, Card } from '@/shared/ui'
import type { FaqCategory } from '../types'

const MARKDOWN_COMPONENTS = {
  p: ({ children }: { children?: React.ReactNode }) => <p className="leading-relaxed">{children}</p>,
  strong: ({ children }: { children?: React.ReactNode }) => (
    <strong className="font-semibold text-zinc-700 dark:text-zinc-200">{children}</strong>
  ),
}

interface FaqCategorySectionProps {
  category: FaqCategory
  forceOpen: boolean
}

export function FaqCategorySection({ category, forceOpen }: FaqCategorySectionProps) {
  return (
    <section id={category.slug} className="scroll-mt-20">
      <Card>
        <div className="mb-1 flex items-baseline justify-between gap-3">
          <h3 className="text-base font-semibold text-zinc-800 dark:text-zinc-100">{category.name}</h3>
          <span className="shrink-0 text-xs text-zinc-400 dark:text-zinc-500">
            {category.items.length} {category.items.length === 1 ? 'pergunta' : 'perguntas'}
          </span>
        </div>
        <div>
          {category.items.map((item) => (
            <AccordionItem key={item.question} summary={item.question} open={forceOpen}>
              <ReactMarkdown components={MARKDOWN_COMPONENTS}>{item.answer}</ReactMarkdown>
            </AccordionItem>
          ))}
        </div>
      </Card>
    </section>
  )
}
