export interface FaqItem {
  question: string
  answer: string
}

export interface FaqCategory {
  name: string
  slug: string
  items: FaqItem[]
}

export interface FaqDocument {
  title: string
  subtitle: string
  categories: FaqCategory[]
}
